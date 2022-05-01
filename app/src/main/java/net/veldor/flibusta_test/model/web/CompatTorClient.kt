package net.veldor.flibusta_test.model.web

import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.config.RegistryBuilder
import cz.msebera.android.httpclient.conn.DnsResolver
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager
import cz.msebera.android.httpclient.ssl.SSLContexts
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.TorHandler
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException

class CompatTorClient {

    fun rawRequest(link: String): HttpResponse? {
        val httpGet = HttpGet(link)
        httpGet.setHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
        )
        val authCookie = PreferencesHandler.instance.authCookie
        if (authCookie != null) {
            httpGet.setHeader("Cookie", authCookie)
        }
        httpGet.setHeader("X-Compress", "null")
        return newHttpClient.execute(httpGet, mContext)
    }

    class FakeDnsResolver : DnsResolver {
        @Throws(UnknownHostException::class)
        override fun resolve(host: String): Array<InetAddress> {
            Log.d("surprise", "MyWebViewClient.kt 97 resolve resolving $host")
            return arrayOf(InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)))
        }
    }

    private val newHttpClient: HttpClient
        get() {
            // check connection
            val reg = RegistryBuilder.create<ConnectionSocketFactory>()
                .register("http", MyConnectionSocketFactory())
                .register("https", MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build()
            val cm = PoolingHttpClientConnectionManager(reg, FakeDnsResolver())
            return HttpClients.custom()
                .setConnectionManager(cm)
                .build()
        }
    private val mContext: HttpClientContext
        get() {
            val port = TorHandler.instance.getPort()
                val socksaddr = InetSocketAddress("127.0.0.1", port)
                val context = HttpClientContext.create()
                context.setAttribute("socks.address", socksaddr)
                return context
        }
}