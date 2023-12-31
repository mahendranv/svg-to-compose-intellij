package by.overpass.svgtocomposeintellij.domain

import java.io.File

interface VectorImageTypeDetector {

    fun detect(path: String): VectorImageType?

    companion object : VectorImageTypeDetector {

        override fun detect(path: String): VectorImageType? {
            val files = File(path)
                .listFiles { file -> !file.isHidden }
                ?: return null
            return if (files.all { file -> file.extension == "svg" }) {
                VectorImageType.SVG
            } else if (files.all { file -> file.extension == "xml" }) {
                VectorImageType.DRAWABLE
            } else {
                null
            }
        }
    }
}
