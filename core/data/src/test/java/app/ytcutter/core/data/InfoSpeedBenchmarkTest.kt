package app.ytcutter.core.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.util.concurrent.TimeUnit

class InfoSpeedBenchmarkTest {

    private class JvmDownloader : Downloader() {
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        override fun execute(request: NpRequest): NpResponse {
            val builder = Request.Builder()
                .url(request.url())
                .header("User-Agent", UA)
                .header("Accept-Language", "en-US,en;q=0.9")
            request.headers().forEach { (name, values) ->
                values.firstOrNull()?.let { builder.header(name, it) }
            }
            val body = request.dataToSend()?.toRequestBody()
            val method = request.httpMethod().uppercase()
            val call = builder.method(method, if (method == "GET" || method == "HEAD") null else body).build()
            client.newCall(call).execute().use { r ->
                return NpResponse(r.code, r.message, r.headers.toMultimap(), r.body?.string(), request.url())
            }
        }
    }

    @Test
    fun benchmark() {
        assumeTrue("set LIVE_TESTS=1", System.getenv("LIVE_TESTS") == "1")
        NewPipe.init(JvmDownloader())
        val url = System.getenv("BENCH_URL") ?: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val rounds = (System.getenv("BENCH_ROUNDS") ?: "3").toInt()

        repeat(rounds) { i ->
            val np = measure {
                val service = NewPipe.getServiceByUrl(url)
                val info = StreamInfo.getInfo(service, url)
                info.videoStreams.size + info.videoOnlyStreams.size + info.audioStreams.size
            }
            println("ROUND $i newpipe_ok=${np.second} newpipe_ms=${np.first}")

            val yt = runCatching {
                measure {
                    val p = ProcessBuilder(YTDLP, "-J", "--no-playlist", "--no-warnings", url)
                        .redirectErrorStream(true)
                        .redirectOutput(File("/dev/null"))
                        .start()
                    p.waitFor(120, TimeUnit.SECONDS)
                    p.exitValue()
                }
            }.getOrNull()
            println("ROUND $i ytdlp_ok=${yt?.second} ytdlp_ms=${yt?.first}")
        }
    }

    private fun measure(block: () -> Any?): Pair<Long, Any?> {
        val t0 = System.nanoTime()
        val result = block()
        return (System.nanoTime() - t0) / 1_000_000 to result
    }

    private companion object {
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        val YTDLP = System.getenv("YTDLP") ?: "/tmp/opencode/yt-dlp"
    }
}
