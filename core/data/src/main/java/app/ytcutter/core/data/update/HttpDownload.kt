package app.ytcutter.core.data.update

import okhttp3.Request
import app.ytcutter.core.network.HttpClient
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/** Downloads one file, verifies size and SHA-256, deletes it on any mismatch. */
internal object HttpDownload {

    fun fetch(
        url: String,
        dest: File,
        expectedSha256: String,
        expectedSize: Long,
        onProgress: (Float) -> Unit = {}
    ): File {
        dest.parentFile?.mkdirs()
        dest.delete()
        val request = Request.Builder().url(url).build()
        HttpClient.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unduhan gagal (HTTP ${response.code})")
            val body = response.body ?: error("Respons kosong")
            val total = body.contentLength().takeIf { it > 0 } ?: expectedSize
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            body.byteStream().use { input ->
                FileOutputStream(dest).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            if (expectedSize > 0 && dest.length() != expectedSize) {
                dest.delete()
                error("Ukuran file tidak cocok")
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(expectedSha256, ignoreCase = true)) {
                dest.delete()
                error("Checksum file tidak cocok")
            }
        }
        return dest
    }
}