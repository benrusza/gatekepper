package benrusza.gatekepper

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import benrusza.gatekepper.Activity.Companion.urlBackup
import benrusza.gatekepper.Activity.Companion.urlToRedirect
import benrusza.gatekepper.Activity.Companion.webView
import benrusza.gatekepper.theme.MyApplicationTheme
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import androidx.core.net.toUri
import benrusza.gatekepper.download.Download
import benrusza.gatekepper.io.DownloadCompletedReceiver
import benrusza.gatekepper.url.UrlGetterYtDl
import benrusza.gatekepper.webview.MyWebView
import benrusza.gatekepper.webview.ReadTextFile


object DownloadState {
    // Flow para emitir la ruta del fichero cuando la descarga se completa
    private val _downloadCompleteFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val downloadCompleteFlow = _downloadCompleteFlow

    fun notifyDownloadComplete(filePath: String) {
        _downloadCompleteFlow.value = filePath
    }

    fun reset() {
        _downloadCompleteFlow.value = null
    }
}



class Activity : ComponentActivity() {

    companion object {
        var urlToRedirect = ""
        var urlBackup = ""
        var webView: MyWebView? = null
    }

    private val downloadCompletedReceiver = DownloadCompletedReceiver()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(
            downloadCompletedReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            RECEIVER_EXPORTED // Es necesario para Android 12+
        )

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
                    urlBackup = appLinkData.toString()
                    // Check if the path contains "instagram.com"
                    if (appLinkData.toString().contains("https://www.instagram.com/reel/")) {
                        val part = appLinkData.toString().split("/reel/")
                        val part2 = part[1].split("/")
                        if (part2.isNotEmpty()) {
                            urlBackup = part[0] + "/reel/" + part2[0] + "?l=1"
                            WebViewScreen(part[0] + "/reel/" + part2[0]+ "?l=1")
                        } else {
                            WebViewScreen(appLinkData.toString()+ "?l=1")
                        }
                        // Check if the path contains "tiktok.com"
                    } else if (appLinkData.toString().contains("tiktok.com")) {
                        if (appLinkData.toString().contains("/video/")) {
                            val part = appLinkData.toString().split("/video/")
                            WebViewScreen("https://www.tiktok.com/embed/v2/" + part[1])
                        } else {

                            WebViewScreen(appLinkData.toString())
                        }

                        //https://www.youtube.com/shorts/ejQHMofjH3Q
                    } else if (appLinkData.toString().contains("youtube.com")) {
                        //
                        if (appLinkData.toString().contains("/video/")) {
                            val part = appLinkData.toString().split("/video/")
                            WebViewScreen("https://www.tiktok.com/embed/v2/" + part[1])
                        } else {

                            WebViewScreen(appLinkData.toString())
                        }
                    } else if(appLinkData.toString().contains("https://www.facebook.com")){
                        if (appLinkData.toString().contains("/videos/")) {
                            WebViewScreen(appLinkData.toString()+ "?l=1")
                        }
                    }else{
                        //https://es.pinterest.com/pin/663084745158577781/
                        WebViewScreen(appLinkData.toString())
                    }


                }
            }
        }
    }

}

@Composable
fun WebViewScreen(url: String = "") {

    val download by lazy { Download() }
    val getUrlYtDl by lazy { UrlGetterYtDl() }

    var isDownloadInitiated by remember { mutableStateOf(false) }
    val downloadedFilePath by DownloadState.downloadCompleteFlow.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            DownloadState.reset()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        MyWebView(context, url, isLoading = {
                            isLoading = it
                        }).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            webView = this
                        }
                    }, update = { webView ->
                        if (webView.url != url) {
                            webView.loadUrl(url)
                        }
                    })

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center) // Centra el indicador
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (downloadedFilePath != null) {
                        statusText = ""
                        Button(onClick = { openDownloadedFile(context, downloadedFilePath!!) }) {
                            Text(text = "Abrir Fichero Descargado")
                        }
                    } else if (urlToRedirect.isNotEmpty() && !isLoading) {
                        Button(
                            onClick = {
                                isDownloadInitiated = true
                                download.startDownload(
                                    context,
                                    urlToRedirect,
                                    "video.mp4"
                                )
                            },
                            enabled = !isDownloadInitiated
                        ) {
                            Text(text = "Download")
                        }
                        Spacer(Modifier.height(8.dp))
                    } else if (!isLoading) {
                        Button(
                            onClick = {
                                isDownloadInitiated = true

                                scope.launch {
                                    getUrlYtDl.getUrl(
                                        context = context,
                                        urlToProcess = urlBackup,
                                        onStatusUpdate = { newStatus -> statusText = newStatus },
                                        result = { ogUrl ->
                                            download.startDownload(context, ogUrl, "video.mp4")
                                        }
                                    )
                                }
                            },
                            enabled = !isDownloadInitiated
                        ) {
                            Text(text = "Get video with yt-dlp")
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (statusText.isNotEmpty()) {
                        Text(text = statusText)
                    }
                }

            }


        }
    }

}

private fun openDownloadedFile(context: Context, filePath: String) {
    val file = File(filePath)
    Log.d("FILE",filePath)
    if (!file.exists()) {Toast.makeText(context, "El fichero no se encuentra.", Toast.LENGTH_SHORT).show()
        return
    }

    try{
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*") // Especificamos que es un video
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Damos permiso temporal de lectura
        }

        context.startActivity(intent)
    }catch (e: Exception){
        e.printStackTrace()
    }
}



private fun readJsFileFromAssets(context: Context, fileName: String): String {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        String(buffer, Charset.defaultCharset())
    } catch (e: Exception) {
        e.printStackTrace()
        "" // Devuelve una cadena vacía si hay un error
    }
}


@JavascriptInterface
fun injectJavaScript(view: WebView?) {
    view?.context?.let { context ->
        val ReadTextFile = ReadTextFile()
        val script = ReadTextFile.read(context, "webview_logic.js")

        if (script.isNotEmpty()) {
            view.evaluateJavascript(script, null)
        }
    }
}

class JavaScriptInterface(private val context: Context) {
    @JavascriptInterface
    fun logHtml(html: String) {
        val maxLogSize = 3000
        for (i in 0..html.length / maxLogSize) {
            val start = i * maxLogSize
            var end = (i + 1) * maxLogSize
            end = if (end > html.length) html.length else end
            Log.d("WebViewHTML", html.substring(start, end))
        }
    }

    @JavascriptInterface
    fun receiveHtml(html: String) {
        // Process the HTML content here
        Log.d("WebView", "Received HTML: $html")
        if (html.contains("No video element found")){
            return
        }
        if (html.contains("blob:https")){
            return
        }

        if (html.contains("tiktokcdn.com"))
            return


        if (html.length > 2)
            urlToRedirect = html.substring(1, html.length - 1)

        if (urlToRedirect.contains("http")) {
            webView?.post { // Call your WebView method here
                if (webView?.url != urlToRedirect) {
                    webView?.loadUrl(urlToRedirect)
                }
            }

        }
    }

    @JavascriptInterface
    fun foundWatchOnInstagram(text: String) {
        Log.d("JavaScriptInterface", "Elemento con clase 'WatchOnInstagram' encontrado. Texto: '$text'")

        // Ejecuta la acción en el hilo principal de la UI
        (context as? ComponentActivity)?.runOnUiThread {
            Toast.makeText(context, "Elemento de Instagram detectado!", Toast.LENGTH_LONG).show()
            webView?.loadUrl(urlBackup);

        }
    }
}

