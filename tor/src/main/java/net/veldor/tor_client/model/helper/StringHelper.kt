package net.veldor.tor_client.model.helper

import java.io.BufferedReader
import java.io.InputStream

object StringHelper {
    fun streamToString(inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        try {
            val reader = BufferedReader(inputStream.reader())
            reader.use { read ->
                return read.readText()
            }
        } catch (_: Throwable) {
            return null
        }
    }
}