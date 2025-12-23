package benrusza.gatekepper.download

import android.content.Context

interface IDownload {
    fun startDownload(context: Context, url: String, fileName: String)
}