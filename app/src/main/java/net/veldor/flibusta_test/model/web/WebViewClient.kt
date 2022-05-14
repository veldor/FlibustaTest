package net.veldor.flibusta_test.model.web

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.delegate.DownloadLinksDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.ui.browser_fragments.WebViewFragment
import java.io.*
import java.nio.charset.StandardCharsets

class WebViewClient internal constructor(
    val context: Context,
    val delegate: DownloadLinksDelegate
) : WebViewClient() {
    var isFullscreen: Boolean = false
    private var mViewMode = 0
    private var mNightMode = false

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse {
        return handleRequest(view, url)!!
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val url = request?.url?.toString()
            if (url != null) {
                if (UrlHelper.isBookDownloadLink(url) && !url.endsWith("read")) {
                    delegate.linkClicked(url)
                    return true
                }
            }
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val requestString = request.url.toString()
        // find for download link
        if (UrlHelper.isBookDownloadLink(requestString) && !requestString.endsWith("read")) {
            delegate.linkClicked(requestString)
            return null
        }
        return handleRequest(view, requestString)
    }


    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    private fun injectMyJs(originalJs: String?): String? {
        Log.d("surprise", "WebViewClient.kt 52: append js")
        var output = originalJs
        try {
            if (mViewMode == WebViewFragment.VIEW_MODE_FAT || mViewMode == WebViewFragment.VIEW_MODE_LIGHT) {
                val inputStream = context.assets.open(
                    MY_JS
                )
                output += inputStreamToString(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return output
    }

    private fun injectMyCss(originalCss: String?): String? {
        // старые версии Android не понимают переменные цветов и новые объявления JS, подключусь в режиме совместимости
        var inputStream: InputStream
        var output = originalCss
        try {
            if (mViewMode > 0) {
                inputStream = when (mViewMode) {
                    WebViewFragment.VIEW_MODE_FAT, WebViewFragment.VIEW_MODE_FAST_FAT -> App.instance.assets.open(
                        MY_COMPAT_FAT_CSS_STYLE
                    )
                    WebViewFragment.VIEW_MODE_LIGHT -> App.instance.assets.open(MY_COMPAT_CSS_STYLE)
                    else -> App.instance.assets.open(MY_COMPAT_CSS_STYLE)
                }
                val cssText = inputStreamToString(inputStream)
                output += cssText
            }
            if (PreferencesHandler.instance.nightMode == PreferencesHandler.NIGHT_THEME_NIGHT || (PreferencesHandler.instance.nightMode != PreferencesHandler.NIGHT_THEME_DAY &&
                        (App.instance.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            ) {
                inputStream = App.instance.assets.open(MY_CSS_NIGHT_STYLE)
                output += inputStreamToString(inputStream)
            }
            if (isFullscreen) {
                inputStream = App.instance.assets.open(MY_CSS_FULLSCREEN_STYLE)
                output += inputStreamToString(inputStream)
            }
            inputStream = App.instance.assets.open(BOOTSTRAP_CSS_STYLE)
            output += inputStreamToString(inputStream)
            return output
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun inputStreamToString(`is`: InputStream): String? {
        try {
            val r = BufferedReader(InputStreamReader(`is`))
            val total = StringBuilder()
            var line: String?
            while (r.readLine().also { line = it } != null) {
                total.append(line).append('\n')
            }
            return total.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun handleRequest(view: WebView, incomingUrl: String): WebResourceResponse? {

        return try {
            mViewMode = PreferencesHandler.instance.browserViewMode
            // обрубаю загрузку картинок в упрощённом виде
            if (mViewMode > 1) {
                val extensionArr = incomingUrl.split("\\.").toTypedArray()
                if (extensionArr.isNotEmpty()) {
                    val extension = extensionArr[extensionArr.size - 1]
                    if (extension == JPG_TYPE || extension == JPEG_TYPE || extension == PNG_TYPE || extension == GIF_TYPE) {
                        return super.shouldInterceptRequest(view, incomingUrl)
                    }
                }
            }
            val httpResponse =
                UniversalWebClient().rawRequest(
                    incomingUrl.replace(UrlHelper.getBaseUrl(), ""),
                    false
                )
            if (httpResponse.statusCode >= 400) {
                delegate.notifyRequestError()
                return connectionError
            }
            var encoding = ENCODING_UTF_8
            var mime = httpResponse.contentType!!
            // если загружена страница- добавлю её как последнюю загруженную
            if (mime.startsWith(HTML_TYPE)) {
                if (!incomingUrl.startsWith(UrlHelper.getBaseUrl() + "/makebooklist?")) {
                    PreferencesHandler.instance.lastWebViewLink = incomingUrl
                    // попробую найти внутри ссылки на книги
                    // скопирую inputStream для разбора ссылок
                    val baos = ByteArrayOutputStream()
                    // Fake code simulating the copy
                    // You can generally do better with nio if you need...
                    // And please, unlike me, do something about the Exceptions :D
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (httpResponse.inputStream!!.read(buffer).also { len = it } > -1) {
                        baos.write(buffer, 0, len)
                    }
                    baos.flush()
                    // Open new InputStreams using the recorded bytes
                    // Can be repeated as many times as you wish
                    val my: InputStream = ByteArrayInputStream(baos.toByteArray())
                    //обработаю текст страницы и найду что-то полезное
                    delegate.textReceived(my)
                    return WebResourceResponse(
                        "text/html",
                        "UTF-8",
                        ByteArrayInputStream(baos.toByteArray())
                    )
                }
            }
            if (mime == CSS_FORMAT) {
                val `is` = httpResponse.inputStream!!
                // подключу нужные CSS простым объединением строк
                val origin = inputStreamToString(`is`)
                val injectionText = injectMyCss(origin)
                if (injectionText != null) {
                    val inputStream = ByteArrayInputStream(
                        injectionText.toByteArray(
                            charset(
                                encoding
                            )
                        )
                    )
                    return WebResourceResponse(mime, ENCODING_UTF_8, inputStream)
                }
                if (origin != null) {
                    val inputStream = ByteArrayInputStream(
                        origin.toByteArray(
                            charset(
                                encoding
                            )
                        )
                    )
                    return WebResourceResponse(mime, ENCODING_UTF_8, inputStream)
                }
                return WebResourceResponse(mime, ENCODING_UTF_8, null)
            } else if (mime == JS_FORMAT) {
                val `is` = httpResponse.inputStream!!
                val origin = inputStreamToString(`is`)
                val injectionText = injectMyJs(origin)
                val inputStream = ByteArrayInputStream(
                    injectionText!!.toByteArray(charset(encoding))
                )
                return WebResourceResponse(mime, ENCODING_UTF_8, inputStream)
            }
            if (mime.contains(";")) {
                var arr = mime.split(";").toTypedArray()
                mime = arr[0]
                arr = arr[1].split("=").toTypedArray()
                encoding = arr[1]
            }
            WebResourceResponse(mime, encoding, httpResponse.inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            connectionError
        }
    }

    /**
     * Сообщу об ошибке соединения и верну заглушку
     */
    private val connectionError: WebResourceResponse
        get() {
            val message = "<H1 style='text-align:center;'>Ошибка подключения к сети</H1>"
            var inputStream: ByteArrayInputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                inputStream = ByteArrayInputStream(message.toByteArray(StandardCharsets.UTF_8))
            } else {
                try {
                    inputStream = ByteArrayInputStream(message.toByteArray(charset(ENCODING_UTF_8)))
                } catch (ex: UnsupportedEncodingException) {
                    ex.printStackTrace()
                }
            }
            return WebResourceResponse("text/html", ENCODING_UTF_8, inputStream)
        }

    companion object {
        private const val ENCODING_UTF_8 = "UTF-8"
        private const val CSS_FORMAT = "text/css"
        private const val JS_FORMAT = "application/javascript"

        // content types
        private const val JPG_TYPE = "jpg"
        private const val JPEG_TYPE = "jpeg"
        private const val GIF_TYPE = "gif"
        private const val PNG_TYPE = "png"
        private const val MY_COMPAT_CSS_STYLE = "myCompatStyle.css"
        private const val BOOTSTRAP_CSS_STYLE = "bootstrap.css"
        private const val MY_CSS_NIGHT_STYLE = "myNightMode.css"
        private const val MY_CSS_FULLSCREEN_STYLE = "forFullscreen.css"
        private const val MY_COMPAT_FAT_CSS_STYLE = "myCompatFatStyle.css"
        private const val MY_JS = "myJs.js"
        private const val HTML_TYPE = "text/html"
    }
}