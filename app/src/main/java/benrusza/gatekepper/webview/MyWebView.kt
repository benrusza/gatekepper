package benrusza.gatekepper.webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import benrusza.gatekepper.Activity.Companion.urlBackup
import benrusza.gatekepper.Activity.Companion.urlToRedirect
import benrusza.gatekepper.JavaScriptInterface
import benrusza.gatekepper.injectJavaScript

class MyWebView(context: Context, url: String, var isLoading : (Boolean) -> Unit) : WebView(context) {

    init {
        webViewClient = object : WebViewClient() {

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)
                isLoading(true)
                Log.d("WebViewScreen", "Page started loading: $url")
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                Log.d("WebViewScreen", "request to: ${request?.url.toString()}")
                Log.d("WebViewScreen", "urlToRedirect to: $urlToRedirect")
                Log.d("WebViewScreen", "urlBackup to: $urlBackup")
                if (urlToRedirect.isNotEmpty()) {
                    Log.d("WebViewScreen", "Redirecting to: $urlToRedirect")
                    view?.stopLoading()
                    view?.loadUrl(urlToRedirect)
                }
                if (request?.url.toString().contains("/video/")) {
                    val part = request?.url.toString().split("/video/")
                    if (part[1].contains("?")) {
                        view?.loadUrl("https://www.tiktok.com/embed/v2/" + part[1].split("?")[0])

                    } else {
                        view?.loadUrl("https://www.tiktok.com/embed/v2/" + part[1])
                    }

                }
                if (request?.url.toString().contains("www.instagram.com/reel")) {
                    return false
                }

                if (request?.url.toString().contains("facebook.com")) {
                    return false
                }
                Log.d("overrideredirect", "true")

                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading(false)

                injectJavaScript(view)
            }
        }
        addJavascriptInterface(JavaScriptInterface(context), "Android")
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        loadUrl(url)
    }
}