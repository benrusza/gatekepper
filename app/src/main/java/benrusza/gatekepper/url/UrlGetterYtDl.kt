package benrusza.gatekepper.url

import android.content.Context
import android.util.Log
import benrusza.gatekepper.R
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UrlGetterYtDl : IUrlGetter {

    override suspend fun getUrl(
        context: Context,
        urlToProcess: String,
        onStatusUpdate: (String) -> Unit,
        result: (String) -> Unit
    )  {
        if (urlToProcess.isBlank()) {
            onStatusUpdate(context.getString(R.string.invalid_url))
            return
        }
        onStatusUpdate(context.getString(R.string.getting_video_info))


        val videoInfo: VideoInfo? = try {
            withContext(Dispatchers.IO) {
                getInstance().getInfo(urlToProcess)
            }
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
            onStatusUpdate(context.getString(R.string.error_prefix, e.cause?.message ?: e.message))
            null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            onStatusUpdate(context.getString(R.string.process_cancelled))
            null
        }
        val downloadUrl = videoInfo?.url ?: videoInfo?.formats?.find {
            it.vcodec != "none" && it.acodec != "none" && it.ext == "mp4"
        }?.url

        if (!downloadUrl.isNullOrBlank()) {

            Log.d("YTDLP_SUCCESS", "URL encontrada: $downloadUrl")
            onStatusUpdate(context.getString(R.string.url_obtained))

            result(downloadUrl)

        } else {
            onStatusUpdate(context.getString(R.string.no_download_link))
            Log.e("YTDLP_FAILURE", "videoInfo fue nulo o no se encontró un formato mp4 válido.")
            videoInfo?.formats?.forEach {
                Log.e("YTDLP_FAILURE", "${it?.url} ")
            }
            Log.e("YTDLP_FAILURE", "${videoInfo?.url} ")
            Log.e("YTDLP_FAILURE", "${videoInfo?.ext} ")
            Log.e("YTDLP_FAILURE", "${videoInfo?.webpageUrl} ")
        }
    }
}