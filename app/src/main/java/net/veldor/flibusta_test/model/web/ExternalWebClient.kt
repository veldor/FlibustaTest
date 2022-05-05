package net.veldor.flibusta_test.model.web

import net.veldor.flibusta_test.model.handler.PreferencesHandler
import java.net.HttpURLConnection
import java.net.URL

const val REQUEST_METHOD_GET = "GET"
const val READ_TIMEOUT_SEC = 15
const val CONNECT_TIMEOUT_SEC = 50
const val USER_AGENT_PROPERTY = "User-Agent"
const val TOR_BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"

object ExternalWebClient {
    @JvmStatic
    fun rawRequest(url: String?): HttpURLConnection? {
        val host = URL(url)
        val connection = host.openConnection() as HttpURLConnection
        val authCookie = PreferencesHandler.instance.authCookie
        if (authCookie != null) {
            connection.setRequestProperty("Cookie", authCookie)
        }
        connection.apply {
            requestMethod = REQUEST_METHOD_GET
            connectTimeout = CONNECT_TIMEOUT_SEC * 1000
            readTimeout = READ_TIMEOUT_SEC * 1000
            connect()
        }
        val code = connection.responseCode
        if (code < 400) {
            // success connection, return input stream
            return connection
        }
        return null
    }
}