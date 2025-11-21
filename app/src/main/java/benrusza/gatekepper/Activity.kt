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

class DownloadCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != -1L) {
                Log.d("DownloadReceiver", "Download with ID $id finished!")
                val filePath = findFileByDownloadId(context, id)?.absolutePath
                if (filePath != null) {
                    // Notificamos al StateFlow que la descarga terminó y tenemos la ruta
                    DownloadState.notifyDownloadComplete(filePath)
                }
            }
        }
    }
}

class Activity : ComponentActivity() {

    companion object {
        var urlToRedirect = ""
        var urlBackup = ""
        var webView: WebView? = null
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
                    }


                }
            }
        }
    }

}

@Composable
fun WebViewScreen(url: String = "") {


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


    Box(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier
            .fillMaxSize()) {

            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : android.webkit.WebViewClient() {

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
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
                                if (request?.url.toString().contains("/video/")){
                                    val part = request?.url.toString().split("/video/")
                                    if (part[1].contains("?")){
                                        view?.loadUrl("https://www.tiktok.com/embed/v2/" + part[1].split("?")[0])

                                    }else{
                                        view?.loadUrl("https://www.tiktok.com/embed/v2/" + part[1])
                                    }

                                }
                                if (request?.url.toString().contains("www.instagram.com/reel")){
                                    return false
                                }

                                if (request?.url.toString().contains("facebook.com")){
                                    return false
                                }
                                Log.d("overrideredirect","true")

                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false

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
                    statusText=""
                    Button(onClick = { openDownloadedFile(context, downloadedFilePath!!) }) {
                        Text(text = "Abrir Fichero Descargado")
                    }
                }
                else if (urlToRedirect.isNotEmpty() && !isLoading) {
                    Button(
                        onClick = {
                            isDownloadInitiated = true
                            startDownload(
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
                }else if(!isLoading){
                    Button(
                        onClick = {
                            isDownloadInitiated = true
                            startYoutubeDLProcess(
                                scope = scope,
                                context = context,
                                urlToProcess = urlBackup,
                                onStatusUpdate = { newStatus -> statusText = newStatus },
                            )
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


private fun startYoutubeDLProcess(
    scope: CoroutineScope,
    context: Context,
    urlToProcess: String,
    onStatusUpdate: (String) -> Unit
) {
    scope.launch {
        if (urlToProcess.isBlank()) {
            onStatusUpdate("No se encontró una URL válida.")
            return@launch
        }
        onStatusUpdate("Obteniendo información del video...")


        val videoInfo: VideoInfo? = try {
            withContext(Dispatchers.IO) {
                getInstance().getInfo(urlToProcess)
            }
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
            onStatusUpdate("Error: ${e.cause?.message ?: e.message}")
            null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            onStatusUpdate("Proceso cancelado.")
            null
        }
            val downloadUrl = videoInfo?.url ?: videoInfo?.formats?.find {
                it.vcodec != "none" && it.acodec != "none" && it.ext == "mp4"
        }?.url

        if (!downloadUrl.isNullOrBlank()) {
            val fileName = (videoInfo?.title ?: "video").replace(Regex("[^a-zA-Z0-9.-]"), "_") + ".mp4"

            Log.d("YTDLP_SUCCESS", "URL encontrada: $downloadUrl")
            onStatusUpdate("¡URL de descarga obtenida! Iniciando...")

            startDownload(context, downloadUrl, fileName)
        } else {
            onStatusUpdate("No se encontró un enlace de video descargable.")
            Log.e("YTDLP_FAILURE", "videoInfo fue nulo o no se encontró un formato mp4 válido.")
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


    try {

    } catch (e: Exception) {
        Log.e("OpenFileError", "No se encontró una aplicación para abrir el video.", e)
        Toast.makeText(context, "No se encontró una aplicación para abrir videos.", Toast.LENGTH_SHORT).show()
    }
}


private fun startDownload(context: Context, url: String, fileName: String) {
    try {
        val request = DownloadManager.Request(url.toUri())
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Descarga iniciada...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al iniciar la descarga.", Toast.LENGTH_LONG).show()
    }
}

private fun findFileByDownloadId(context: Context, downloadId: Long): File? {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val query = DownloadManager.Query().setFilterById(downloadId)
    val cursor = downloadManager.query(query)
    if (cursor.moveToFirst()) {
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            if (uriString != null) {
                return File(uriString.toUri().path!!)
            }
        }
    }
    return null
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
        // 1. Lee el contenido del fichero JS
        val script = readJsFileFromAssets(context, "webview_logic.js")

        // 2. Ejecuta el script leído
        if (script.isNotEmpty()) {
            view.evaluateJavascript(script, null)
        }
    }
}

class JavaScriptInterface(private val context: Context) {
    @JavascriptInterface
    fun logHtml(html: String) {
        // Logcat tiene un límite de caracteres por línea (aprox. 4000).
        // Si el HTML es muy largo, lo dividimos en trozos para imprimirlo completo.
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

