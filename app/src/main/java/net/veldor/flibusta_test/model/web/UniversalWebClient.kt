package net.veldor.flibusta_test.model.web

import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.selections.web.WebResponse

class UniversalWebClient {
    fun rawRequest(request: String): WebResponse {
        return rawRequest(UrlHelper.getBaseUrl(), request)
    }

    fun rawRequest(mirror: String, request: String): WebResponse {
        try {
            val requestString = mirror + request
            Log.d("surprise", "UniversalWebClient.kt 19: request $requestString")
            if (!PreferencesHandler.instance.useTor) {
                val response = ExternalWebClient.rawRequest(requestString)
                return if (response != null) {
                    val headers = response.headerFields
                    val resultHeaders = HashMap<String, String>()
                    headers.forEach {
                        if (it.key != null) {
                            val value = if (it.value.isNotEmpty()) {
                                it.value[0]
                            } else {
                                ""
                            }
                            resultHeaders[it.key] = value
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        WebResponse(
                            response.responseCode,
                            response.inputStream,
                            response.contentType,
                            resultHeaders,
                            response.contentLength,
                            response.contentLengthLong
                        )
                    } else {
                        WebResponse(
                            response.responseCode,
                            response.inputStream,
                            response.contentType,
                            resultHeaders,
                            response.contentLength,
                            response.contentLength.toLong()
                        )
                    }
                } else {
                    WebResponse(999, null, null, null)
                }
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                val response = CompatTorClient().rawRequest(requestString)
                return if (response != null) {
                    val resultHeaders = HashMap<String, String>()
                    response.allHeaders.forEach {
                        resultHeaders[it.name] = it.value
                    }
                    WebResponse(
                        response.statusLine.statusCode,
                        response.entity.content,
                        response.entity.contentType.value,
                        resultHeaders,
                        response.entity.contentLength.toInt(),
                        response.entity.contentLength
                    )
                } else {
                    WebResponse(999, null, null, null)
                }
            }
            val response = TorClient.rawRequest(requestString)
            return if (response != null) {
                val headers = response.headerFields
                val resultHeaders = HashMap<String, String>()
                headers.forEach {
                    if (it.key != null) {
                        val value = if (it.value.isNotEmpty()) {
                            it.value[0]
                        } else {
                            ""
                        }
                        resultHeaders[it.key] = value
                    }
                }
                WebResponse(
                    response.responseCode,
                    response.inputStream,
                    response.contentType,
                    resultHeaders,
                    response.contentLength,
                    response.contentLengthLong
                )
            } else {
                WebResponse(999, null, null, null)
            }
        } catch (e: Throwable) {
            connectionError.postValue(e)
            return WebResponse(999, null, null, null)
        }
    }

    companion object {
        val connectionError = MutableLiveData<Throwable>()
    }
}