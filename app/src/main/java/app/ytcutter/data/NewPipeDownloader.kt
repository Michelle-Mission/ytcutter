package app.ytcutter.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class NewPipeDownloader(context: Context) : Downloader() {

    private val appContext = context.applicationContext

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")

        cookies()?.let { builder.header("Cookie", it) }
        request.headers().forEach { (name, values) ->
            if (!name.equals("Cookie", ignoreCase = true)) {
                values.firstOrNull()?.let { builder.header(name, it) }
            }
        }

        val body = request.dataToSend()?.toRequestBody()
        val method = request.httpMethod().uppercase()
        val call = builder.method(method, if (method == "GET" || method == "HEAD") null else body).build()
        client.newCall(call).execute().use { response ->
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string(),
                request.url()
            )
        }
    }

    private fun cookies(): String? {
        val file = CookieStore.file(appContext)
        if (!file.exists() || file.length() == 0L) return null
        val pairs = file.readLines()
            .asSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size >= 7) "${parts[5]}=${parts[6]}" else null
            }
            .joinToString("; ")
        return pairs.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
