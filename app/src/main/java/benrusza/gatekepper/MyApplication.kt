package benrusza.gatekepper

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
         CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@MyApplication)
                Log.d("YTDLP_INIT", "yt-dlp inicializado correctamente.")
            } catch (e: YoutubeDLException) {
                Log.e("YTDLP_INIT", "Error al inicializar yt-dlp", e)
            }
        }
    }
}
