package app.ytcutter.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class RemoteConfigLiveTest {

    @Test
    fun `pages config parses`() {
        assumeTrue("set LIVE_TESTS=1 untuk uji jaringan", System.getenv("LIVE_TESTS") == "1")
        val url = "https://nurfilardi.github.io/ytcutter/config.json"
        val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).build()
        val body = try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                assumeTrue("HTTP ${r.code}", r.isSuccessful)
                r.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            assumeTrue("offline: ${e.message}", false)
            ""
        }
        val config = RemoteConfigApi("nurfilardi", "ytcutter").parseConfig(body)
        assertTrue("config parsed", config != null)
        assertEquals(false, config!!.maintenance.enabled)
        println("announcement=${config.announcement}")
    }
}
