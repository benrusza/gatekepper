package benrusza.gatekepper.webview

import android.content.Context

interface IReadTextFile {
    fun read(context: Context, fileName: String): String
}