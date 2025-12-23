package benrusza.gatekepper.io

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import benrusza.gatekepper.DownloadState
import java.io.File

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
