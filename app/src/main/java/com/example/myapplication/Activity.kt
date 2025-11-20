package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.Activity.Companion.urlToRedirect
import com.example.myapplication.Activity.Companion.webView
import com.example.myapplication.ui.theme.MyApplicationTheme

class Activity : ComponentActivity() {

    companion object {
        var urlToRedirect = ""
        var webView: WebView? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        handleIntent(intent)


    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.stopLoading()
        webView?.destroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        urlToRedirect = ""
        val appLinkAction = intent.action
        val appLinkData: Uri? = intent.data
        if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
            val path = appLinkData.lastPathSegment


            enableEdgeToEdge()
            setContent {
                MyApplicationTheme {
                    // Check if the path contains "instagram.com"
                    if (appLinkData.toString().contains("https://www.instagram.com/reel/")) {
                        val part = appLinkData.toString().split("/reel/")
                        val part2 = part[1].split("/")
                        if (part2.isNotEmpty()) {
                            WebViewScreen(part[0] + "/reel/" + part2[0] + "/embed/")
                        } else {
                            WebViewScreen(appLinkData.toString() + "/embed/")
                        }
                    // Check if the path contains "tiktok.com"
                    } else if (appLinkData.toString().contains("tiktok.com")) {
                        if (appLinkData.toString().contains("/video/")) {
                            val part = appLinkData.toString().split("/video/")
                            WebViewScreen("https://www.tiktok.com/embed/v2/" + part[1])
                        }else{

                            WebViewScreen(appLinkData.toString())
                        }

                        //https://www.youtube.com/shorts/ejQHMofjH3Q
                    }else if (appLinkData.toString().contains("youtube.com")) {
                        //
                        if (appLinkData.toString().contains("/video/")) {
                            val part = appLinkData.toString().split("/video/")
                            WebViewScreen("https://www.tiktok.com/embed/v2/" + part[1])
                        }else{

                            WebViewScreen(appLinkData.toString())
                        }
                    }



                }
            }
        }
    }

}

@Composable
fun WebViewScreen(url: String = "") {

    var text by remember { mutableStateOf("Loading...") }

    Box(modifier = Modifier.fillMaxSize()) {

        if (urlToRedirect.isNotEmpty()) {
            Button(onClick = {

            }) {
                text = "Download"
            }
        }


        Column {
            Spacer(Modifier.height(16.dp))
            Text(text = text)
            Spacer(Modifier.height(16.dp))
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                Log.d("WebViewScreen", "Page started loading: $url")
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?, request: WebResourceRequest?
                            ): Boolean {
                                Log.d("WebViewScreen", "request to: $request")
                                if (urlToRedirect.isNotEmpty()) {
                                    Log.d("WebViewScreen", "Redirecting to: $urlToRedirect")
                                    view?.stopLoading()
                                    view?.loadUrl(urlToRedirect)
                                }
                                if (request?.url.toString().contains("/video/")){
                                    val part = request?.url.toString().split("/video/")
                                    if (part[1].contains("?")){
                                        view?.loadUrl("https://www.tiktok.com/embed/v2/" + part[1].split("?")[0])

                                    }else{
                                        view?.loadUrl("https://www.tiktok.com/embed/v2/" + part[1])
                                    }

                                }

                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (urlToRedirect != "") {
                                    text = "Page loaded "
                                }

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

                        Activity.webView = this
                    }
                }, update = { webView ->
                    webView.loadUrl(url)
                })
        }


    }

}


fun getEmbed(url: String): String {
    return "<html>" + "<body>" + "<div>$url</div>" + "</body>" + "</html>"
}


fun getHtmlBase(embedCode: String): String {
    return """
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <body style="margin:0; padding:0;">
        $embedCode
    </body>
    </html>
    """

}

fun getHtmlVideo(url: String): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Your Video Page</title>
        </head>
        <body>
            <iframe
  width="300"
  height="200"
  src="$url">
</iframe>
        </body>
        </html>

    """.trimIndent()
}


@JavascriptInterface
fun injectJavaScript(view: WebView?) {
    view?.evaluateJavascript(
        """
        (function() {
            const elements = document.querySelectorAll(".Content EmbedFrame");

            elements.forEach(element => {
                element.click();
            });
        })();
        (function() {
            var observer = new MutationObserver(function(mutations) {
                var videoElements = document.getElementsByTagName('video');
            if (videoElements.length > 0) {
                var video = videoElements[0];
                var videoInfo = {
                    src: video.src,
                    width: video.width,
                    height: video.height,
                    autoplay: video.autoplay,
                    controls: video.controls
                };
               
                Android.receiveHtml(JSON.stringify(videoInfo.src));
            } else {
                Android.receiveHtml('No video element found');
            }
            });
            
            var config = { attributes: true, childList: true, characterData: true, subtree: true };
            observer.observe(document.body, config);
        })();
        
      
    """, null
    )
}

class JavaScriptInterface(private val context: Context) {
    @JavascriptInterface
    fun receiveHtml(html: String) {
        // Process the HTML content here
        Log.d("WebView", "Received HTML: $html")
        if (html.contains("tiktokcdn.com"))
            return
        if (html.length > 2)
            urlToRedirect = html.substring(1, html.length - 1)

        if (urlToRedirect.contains("http")) {
            webView?.post { // Call your WebView method here
                webView?.loadUrl(urlToRedirect)
            }

        }
    }
}

