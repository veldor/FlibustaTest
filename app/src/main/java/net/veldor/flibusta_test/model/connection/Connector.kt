package net.veldor.flibusta_test.model.connection

import android.os.Build
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.tor_client.model.connection.TorClient
import net.veldor.tor_client.model.connection.WebResponse
import net.veldor.tor_client.model.managers.ConnectionManager
import java.net.HttpURLConnection
import java.net.URL

class Connector {
    fun rawRequest(request: String, dropConnectionAfterResponse: Boolean): WebResponse {
        val cookie = PreferencesHandler.authCookie
        if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_TOR) {
            try {
                while (TorHandler.liveTorLaunchInProgress.value == true){
                    Thread.sleep(100)
                }
                if(TorHandler.liveTorBootstrapped.value == false){
                    TorHandler.stopTor()
                    Thread.sleep(3000)
                    TorHandler.launchTor()
                }
                return TorClient().rawRequest(
                    "${UrlHelper.getBaseUrl()}/$request",
                    dropConnectionAfterResponse,
                    cookie
                )
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        return rawRequest(UrlHelper.getBaseUrl(), request, dropConnectionAfterResponse, cookie)
    }

    private fun rawRequest(
        mirror: String,
        request: String,
        dropConnectionAfterResponse: Boolean,
        cookie: String? = null
    ): WebResponse {
        return try {
            val requestString = mirror + request
            ConnectionManager().directConnect(requestString, dropConnectionAfterResponse, cookie)
        } catch (e: Throwable) {
            e.printStackTrace()
            WebResponse(999, null, null, hashMapOf(), errorText = e.message)
        }
    }

    fun requestLogin(login: String, password: String): WebResponse {
        val loginUrl = "${UrlHelper.getBaseUrl()}/opds/polka"
        if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_TOR) {
            try {
                while (TorHandler.liveTorLaunchInProgress.value == true){
                    Thread.sleep(100)
                }
                if(TorHandler.liveTorBootstrapped.value == false){
                    TorHandler.stopTor()
                    Thread.sleep(3000)
                    TorHandler.launchTor()
                }
                return TorClient().loginWithBaseAuthorization(loginUrl, login, password)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

            val url = URL(loginUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                connection.setRequestProperty("Authorization", "Basic " + android.util.Base64.encodeToString("$login:$password".toByteArray(), android.util.Base64.DEFAULT))
                readTimeout = 30000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"
                )
                connect()
            }
            val headersArray = HashMap<String, String>()
            connection.
            headerFields?.
            entries?.forEach {
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
            return response
    }

}