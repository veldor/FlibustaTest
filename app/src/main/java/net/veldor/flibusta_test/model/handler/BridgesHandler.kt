package net.veldor.flibusta_test.model.handler

import net.veldor.flibusta_test.App
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class BridgesHandler {
    fun getBridges() {
        // make a request, read file and return data from it
        val host =
            URL("https://gist.githubusercontent.com/veldor/bff3b895fcf7eed9ffb84c76b1fe633d/raw")
        val connection = host.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 3000
            readTimeout = 3000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"
            )
            connect()
        }
        val code = connection.responseCode
        if (code == 200) {
            // get text
            val reader = BufferedReader(connection.inputStream.reader())
            reader.use { read ->
                val bridges = read.readText()
                saveBridgesToFile(bridges)
            }
        }
    }

    fun saveBridgesToFile(bridges: String) {
        val bridgesFile = File(App.instance.filesDir, "bridges")
        val fileOutputStream = FileOutputStream(bridgesFile, false)
        fileOutputStream.write(
            bridges.toByteArray()
        )
        fileOutputStream.close()
    }
}