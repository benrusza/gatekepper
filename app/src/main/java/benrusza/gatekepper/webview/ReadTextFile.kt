package benrusza.gatekepper.webview

import android.content.Context
import java.io.InputStream
import java.nio.charset.Charset

class ReadTextFile : IReadTextFile {
    override fun read(context: Context, fileName: String): String {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.defaultCharset())
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
