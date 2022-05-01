package net.veldor.flibusta_test.model.web

import android.util.Log
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.TorHandler
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

object TorClient {
    @JvmStatic
    fun rawRequest(url: String): HttpURLConnection? {
        var connectionTryCounter = 2
        while (connectionTryCounter > 0){
            Log.d("surprise", "rawRequest: try to connect $connectionTryCounter")
            if(TorHandler.instance.checkTorConnection()){break}
            connectionTryCounter--
        }
        if(connectionTryCounter == 0){
            Log.d("surprise", "rawRequest: no connect")
            return null
        }
        Log.d("surprise", "rawRequest: go next")
        val port = TorHandler.instance.getPort()
        val proxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(
                "127.0.0.1",
                port
            )
        )
        val host = URL(url)
        val connection = host.openConnection(proxy) as HttpURLConnection
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
            Log.d("surprise", "rawRequest: tor connect success")
            // success connection, return input stream
            return connection
        }
        Log.d("surprise", "rawRequest: tor connect failed")
        return null
    }
}