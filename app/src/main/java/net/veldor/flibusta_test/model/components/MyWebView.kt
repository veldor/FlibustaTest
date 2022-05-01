package net.veldor.flibusta_test.model.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.web.WebViewClient
import net.veldor.flibusta_test.ui.BrowserActivity
import net.veldor.flibusta_test.ui.browser_fragments.WebViewFragment

class MyWebView(context: Context?, attrs: AttributeSet?) : WebView(context, attrs) {
    private var init = false

    @SuppressLint("SetJavaScriptEnabled")
    fun setup() {
        if (!this.isInEditMode) {
            Log.d("surprise", "MyWebView.kt 19: setup webview")
            this.webViewClient = WebViewClient(context)
            val webSettings = this.settings
            webSettings.javaScriptEnabled = true
            webSettings.allowFileAccess = true
            webSettings.builtInZoomControls = true
            webSettings.displayZoomControls = false
        }
    }


    override fun loadUrl(url: String) {
        super.loadUrl(UrlHelper.getBaseUrl() + url)
        Log.d("surprise", "MyWebView.kt 30 loadUrl webView load $url")
        initProgressBar()
    }

    private fun initProgressBar() {
        if (init) {
            return
        }
        init = true
        val fragment = (context as BrowserActivity).getCurrentFragment()
        if (fragment is WebViewFragment) {
            fragment.binding.pageLoadedProgressBar.visibility = GONE
            this.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, progress: Int) {
                    if (progress > 90) {
                        if (fragment.binding.pageLoadedProgressBar.visibility == VISIBLE) {
                            fragment.binding.pageLoadedProgressBar.visibility = GONE
                        }
                    } else {
                        if (fragment.binding.pageLoadedProgressBar.visibility == GONE) {
                            fragment.binding.pageLoadedProgressBar.visibility = VISIBLE
                        }
                        fragment.binding.pageLoadedProgressBar.progress = progress
                    }
                }
            }
        }
    }
}