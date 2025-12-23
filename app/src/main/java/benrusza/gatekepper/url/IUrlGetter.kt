package benrusza.gatekepper.url

import android.content.Context

interface IUrlGetter {
    suspend fun getUrl(
        context: Context,
        urlToProcess: String,
        onStatusUpdate: (String) -> Unit,
        result: (String) -> Unit
    )
}