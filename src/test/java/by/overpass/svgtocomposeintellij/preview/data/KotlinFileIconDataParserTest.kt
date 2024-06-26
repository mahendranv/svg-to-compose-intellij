package by.overpass.svgtocomposeintellij.preview.data

import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class KotlinFileIconDataParserTest {

    @Test
    fun `Ab-testing icon is parsed`() = runTest {
        val file = File("src/test/resources/kotlin/Ab-testing.kt")
        val parser = KotlinFileIconDataParser(file)

        val actual = parser.parse()

        assert(actual.isSuccess)
    }

    @Test
    fun `Note icon is parsed`() = runTest {
        val file = File("src/test/resources/kotlin/Note.kt")
        val parser = KotlinFileIconDataParser(file)

        val actual = parser.parse()

        assert(actual.isSuccess)
    }

    @Test
    fun `Clear icon is NOT parsed`() = runTest {
        val file = File("src/test/resources/kotlin/Clear.kt")
        val parser = KotlinFileIconDataParser(file)

        val actual = parser.parse()

        assert(actual.isFailure)
    }

    @Test
    fun `__MyIconPack file is NOT parsed`() = runTest {
        val file = File("src/test/resources/kotlin/Clear.kt")
        val parser = KotlinFileIconDataParser(file)

        val actual = parser.parse()

        assert(actual.isFailure)
    }
}
