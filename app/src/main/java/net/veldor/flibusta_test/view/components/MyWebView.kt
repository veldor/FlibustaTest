package net.veldor.flibusta_test.view.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import net.veldor.flibusta_test.model.connection.WebViewClient
import net.veldor.flibusta_test.model.delegate.DownloadLinksDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.view.SearchActivity
import net.veldor.flibusta_test.view.search_fragment.WebViewFragment
import net.veldor.flibusta_test.view.search_fragment.WebViewFragment.Companion.VIEW_MODE_NORMAL

class MyWebView(context: Context, attrs: AttributeSet?) : WebView(context, attrs) {
    lateinit var client: WebViewClient
    private var init = false

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(delegate: DownloadLinksDelegate) {
        if (!this.isInEditMode) {
            client = WebViewClient(context, delegate)
            this.webViewClient = client
            val webSettings = this.settings
            webSettings.javaScriptEnabled = true
            webSettings.allowFileAccess = true
            webSettings.builtInZoomControls = true
            webSettings.displayZoomControls = false
        }
        if (PreferencesHandler.browserViewMode == VIEW_MODE_NORMAL) {
            settings.useWideViewPort =
                false //make sure this method is false so setInitialScale() can work correctly
            settings.loadWithOverviewMode = true
            setInitialScale(150)
        }

    }


    override fun loadUrl(url: String) {
        if (url.startsWith("javascript:")) {
            super.loadUrl(url)
        } else {
            super.loadUrl(UrlHelper.getBaseUrl() + url)
        }
        initProgressBar()
    }

    private fun initProgressBar() {
        if (init) {
            return
        }
        init = true
        val fragment = (context as SearchActivity).getCurrentFragment()
        if (fragment is WebViewFragment) {
            this.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, progress: Int) {
                    fragment.binding.pageLoadedProgressBar.visibility = View.VISIBLE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        fragment.binding.pageLoadedProgressBar.setProgress(progress, true)
                    } else {
                        fragment.binding.pageLoadedProgressBar.progress = progress
                    }
                    if (progress == 100) {
                        fragment.activity?.runOnUiThread {
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                fragment.binding.pageLoadedProgressBar.progress = 0
                                fragment.binding.pageLoadedProgressBar.visibility = View.GONE
                            }, 300)
                        }
                    }

                }
            }
        }
    }
}