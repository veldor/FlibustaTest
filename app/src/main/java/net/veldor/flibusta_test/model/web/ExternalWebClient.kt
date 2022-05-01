package net.veldor.flibusta_test.model.web

import net.veldor.flibusta_test.model.handler.PreferencesHandler
import java.net.HttpURLConnection
import java.net.URL

const val REQUEST_METHOD_GET = "GET"
const val READ_TIMEOUT_SEC = 15
const val CONNECT_TIMEOUT_SEC = 50
const val USER_AGENT_PROPERTY = "User-Agent"
const val TOR_BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"

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
            setRequestProperty(USER_AGENT_PROPERTY, TOR_BROWSER_USER_AGENT)
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