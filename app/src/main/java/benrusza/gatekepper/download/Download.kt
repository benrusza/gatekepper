package benrusza.gatekepper.download

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri

class Download : IDownload {
    override fun startDownload(
        context: Context,
        url: String,
        fileName: String
    ) {
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
}