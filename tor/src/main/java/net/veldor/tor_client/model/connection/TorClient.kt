package net.veldor.tor_client.model.connection

import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.config.RequestConfig
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.config.RegistryBuilder
import cz.msebera.android.httpclient.conn.DnsResolver
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager
import cz.msebera.android.httpclient.ssl.SSLContexts
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit


class TorClient {

    fun rawRequest(
        link: String,
        dropConnectionAfterResponse: Boolean = false,
        cookie: String? = null
    ): WebResponse {
        val httpGet = HttpGet(link)
        httpGet.setHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
        )
        if (cookie != null) {
            httpGet.setHeader(
                "Cookie",
                cookie
            )
        }
        httpGet.setHeader("X-Compress", "null")
        val result = newHttpClient.execute(httpGet, mContext)
        val headers = result.allHeaders
        val headersArray = HashMap<String, String>()
        headers.forEach {
            headersArray[it.name] = it.value
        }
        if (dropConnectionAfterResponse) {
            result.entity.content.close()
        }
        return WebResponse(
            result.statusLine.statusCode,
            result.entity.content,
            result.entity.contentType.value,
            headersArray,
            result.entity.contentLength.toInt(),
            result.entity.contentLength
        )
    }

    fun loginWithBaseAuthorization(link: String, login: String, password: String): WebResponse {
        val httpGet = HttpGet(link)
        httpGet.setHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
        )
        httpGet.addHeader(
            "Authorization",
            "Basic " + android.util.Base64.encodeToString(
                "$login:$password".toByteArray(),
                android.util.Base64.DEFAULT
            )
        )
        httpGet.setHeader("X-Compress", "null")
        val result = newHttpClient.execute(httpGet, mContext)
        val headers = result.allHeaders
        val headersArray = HashMap<String, String>()
        headers.forEach {
            headersArray[it.name] = it.value
        }
        return WebResponse(
            result.statusLine.statusCode,
            result.entity.content,
            result.entity.contentType.value,
            headersArray,
            result.entity.contentLength.toInt(),
            result.entity.contentLength
        )
    }

    class FakeDnsResolver : DnsResolver {
        @Throws(UnknownHostException::class)
        override fun resolve(host: String): Array<InetAddress> {
            return arrayOf(InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)))
        }
    }

    private val newHttpClient: HttpClient
        get() {
            val timeout = 30
            val rc = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout * 1000)
                .setConnectTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build()
            // check connection
            val reg = RegistryBuilder.create<ConnectionSocketFactory>()
                .register("http", MyConnectionSocketFactory())
                .register("https", MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build()
            val cm = PoolingHttpClientConnectionManager(reg, FakeDnsResolver())
            return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(rc)
                .setConnectionTimeToLive(5, TimeUnit.SECONDS)
                .build()
        }
    private val mContext: HttpClientContext
        get() {
            val socketAddress = InetSocketAddress("127.0.0.1", 9050)
            val context = HttpClientContext.create()
            context.setAttribute("socks.address", socketAddress)
            return context
        }
}