package net.veldor.flibusta_test.model.helper

import java.io.BufferedReader
import java.io.InputStream

object StringHelper {
    fun streamToString(inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        val reader = BufferedReader(inputStream.reader())
        reader.use { read ->
            return read.readText()
        }
    }
}