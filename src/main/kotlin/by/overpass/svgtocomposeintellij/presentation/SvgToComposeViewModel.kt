package by.overpass.svgtocomposeintellij.presentation

import by.overpass.svgtocomposeintellij.domain.SvgIconsGenerator
import by.overpass.svgtocomposeintellij.domain.SvgToComposeData
import by.overpass.svgtocomposeintellij.domain.VectorImageType
import by.overpass.svgtocomposeintellij.domain.VectorImageTypeDetector
import by.overpass.svgtocomposeintellij.presentation.validation.DirError
import by.overpass.svgtocomposeintellij.presentation.validation.Validatable
import by.overpass.svgtocomposeintellij.presentation.validation.ValidationResult
import by.overpass.svgtocomposeintellij.presentation.validation.ValueValidator
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface SvgToComposeViewModel {

    val state: StateFlow<SvgToComposeState>

    fun onAccessorNameChanged(accessorName: String)

    fun onOutputDirChanged(outputDir: String)

    fun onVectorImagesDirChanged(vectorImagesDir: String)

    fun onVectorImageTypeChanged(vectorImageType: VectorImageType)

    fun onAllAssetsPropertyNameChanged(allAssetsPropertyName: String)

    fun generate()

    fun onCleared()
}

class SvgToComposeViewModelImpl(
    targetDir: File,
    private val svgIconsGenerator: SvgIconsGenerator,
    private val vectorImageTypeDetector: VectorImageTypeDetector,
    private val nonStringEmptyValidator: ValueValidator<String, Unit>,
    private val directoryValidator: ValueValidator<String, DirError>,
    dispatcher: CoroutineDispatcher,
) : SvgToComposeViewModel {

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    override val state = MutableStateFlow<SvgToComposeState>(
        DataInput(
            outputDir = Validatable(value = targetDir.path),
        ),
    )

    override fun onAccessorNameChanged(accessorName: String) {
        updateInput { old ->
            val validationResult = nonStringEmptyValidator.validate(accessorName)
            old.copy(
                accessorName = old.accessorName.copy(
                    value = accessorName,
                    isValid = validationResult == ValidationResult.Ok,
                    error = when (validationResult) {
                        is ValidationResult.Ok -> null
                        is ValidationResult.Error -> Unit
                    },
                ),
            )
        }
    }

    override fun onOutputDirChanged(outputDir: String) {
        updateInput { old ->
            val validationResult = directoryValidator.validate(outputDir)
            old.copy(
                outputDir = old.outputDir.copy(
                    value = outputDir,
                    isValid = validationResult == ValidationResult.Ok,
                    error = when (validationResult) {
                        is ValidationResult.Ok -> null
                        is ValidationResult.Error -> validationResult.error
                    },
                ),
            )
        }
    }

    override fun onVectorImagesDirChanged(vectorImagesDir: String) {
        updateInput { old ->
            val validationResult = directoryValidator.validate(vectorImagesDir)
            old.copy(
                vectorImagesDir = old.vectorImagesDir.copy(
                    value = vectorImagesDir,
                    isValid = validationResult == ValidationResult.Ok,
                    error = when (validationResult) {
                        is ValidationResult.Ok -> {
                            detectVectorImagesType(vectorImagesDir)
                            null
                        }
                        is ValidationResult.Error -> validationResult.error
                    },
                ),
            )
        }
    }

    private fun detectVectorImagesType(vectorImagesDir: String) {
        vectorImageTypeDetector.detect(vectorImagesDir)
            ?.let(::onVectorImageTypeChanged)
    }

    override fun onVectorImageTypeChanged(vectorImageType: VectorImageType) {
        updateInput { old ->
            old.copy(vectorImageType = vectorImageType)
        }
    }

    override fun onAllAssetsPropertyNameChanged(allAssetsPropertyName: String) {
        updateInput { old ->
            val validationResult = nonStringEmptyValidator.validate(allAssetsPropertyName)
            old.copy(
                allAssetsPropertyName = old.allAssetsPropertyName.copy(
                    value = allAssetsPropertyName,
                    isValid = validationResult == ValidationResult.Ok,
                    error = when (validationResult) {
                        is ValidationResult.Ok -> null
                        is ValidationResult.Error -> Unit
                    },
                ),
            )
        }
    }

    override fun generate() {
        coroutineScope.launch {
            updateInput { old -> old.copy(isInProgress = true) }
            val currentState = state.value
            if (currentState is DataInput) {
                svgIconsGenerator.generate(
                    SvgToComposeData(
                        applicationIconPackage = "",
                        accessorName = currentState.accessorName.value,
                        outputDir = File(currentState.outputDir.value),
                        vectorsDir = File(currentState.vectorImagesDir.value),
                        vectorImageType = currentState.vectorImageType,
                        allAssetsPropertyName = currentState.allAssetsPropertyName.value,
                    )
                ).fold(
                    onSuccess = {
                        state.value = Finished
                    },
                    onFailure = { throwable ->
                        state.value = Error(throwable)
                    }
                )
            }
        }
    }

    override fun onCleared() {
        coroutineScope.cancel()
    }

    private inline fun updateInput(dataInputUpdater: (DataInput) -> DataInput) {
        state.update { old ->
            if (old is DataInput) {
                dataInputUpdater(old)
            } else {
                old
            }
        }
    }
}
