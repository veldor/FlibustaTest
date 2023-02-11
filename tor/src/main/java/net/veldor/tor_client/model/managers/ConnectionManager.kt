package net.veldor.tor_client.model.managers

import android.os.Build
import net.veldor.tor_client.model.connection.WebResponse
import java.net.HttpURLConnection
import java.net.URL

class ConnectionManager {
    fun directConnect(
        link: String,
        dropConnectionAfterResponse: Boolean = false,
        cookie: String? = null
    ): WebResponse {
        val url = URL(link)
        val connection = url.openConnection() as HttpURLConnection
        if(cookie != null){
            connection.setRequestProperty("Cookie", cookie)
        }
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 30000
            readTimeout = 30000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"
            )
            connect()
        }
        val headersArray = HashMap<String, String>()
        connection.headerFields?.entries?.forEach {
            if (it.key != null) {
                try {
                    headersArray[it.key] = it.value[0]
                } catch (e: ClassCastException) {
                    var result = ""
                    it.value.forEach { headerValue ->
                        result = headerValue
                    }
                    headersArray[it.key] = result
                }
            }
        }
        val response =
            WebResponse(
                connection.responseCode,
                connection.inputStream ?: null,
                connection.contentType ?: null,
                headersArray,
                connection.contentLength,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connection.contentLengthLong
                } else {
                    connection.contentLength.toLong()
                }
            )
        if (dropConnectionAfterResponse) {
            connection.disconnect()
        }
        return response
    }
}