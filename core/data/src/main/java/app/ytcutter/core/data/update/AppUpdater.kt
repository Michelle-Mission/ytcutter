package app.ytcutter.core.data.update

import android.content.Context
import app.ytcutter.core.model.AppUpdate
import app.ytcutter.core.model.UpdateAsset
import app.ytcutter.core.network.UpdateManifestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class AppUpdater(private val context: Context) {

    private val api = UpdateManifestApi(OWNER, REPO)

    fun installedVersionCode(): Long = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrDefault(0L)

    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        api.latest()?.takeIf { it.isNewerThan(installedVersionCode()) }
    }

    /**
     * Returns an APK file ready to install. Uses a bsdiff patch when the
     * installed build is the patch base; otherwise (or on failure) downloads
     * the full APK.
     */
    suspend fun prepare(
        update: AppUpdate,
        onStatus: (String) -> Unit = {},
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "update").apply {
            deleteRecursively()
            mkdirs()
        }
        val installed = installedVersionCode()
        val patch = update.patch?.takeIf { it.fromVersionCode == installed }
        if (patch != null) {
            val patched = runCatching {
                onStatus("Mengunduh patch ${mb(patch.asset.size)}")
                val patchFile = HttpDownload.fetch(
                    patch.asset.url,
                    File(dir, "update.patch"),
                    patch.asset.sha256,
                    patch.asset.size,
                    onProgress
                )
                onStatus("Menyusun APK baru")
                val out = File(dir, "patched.apk")
                BsPatch.apply(File(context.applicationInfo.sourceDir), patchFile, out)
                require(out.matches(update.apk)) { "Hasil patch tidak cocok" }
                patchFile.delete()
                out
            }.getOrNull()
            if (patched != null) return@withContext patched
            File(dir, "patched.apk").delete()
        }
        onStatus("Mengunduh APK penuh ${mb(update.apk.size)}")
        HttpDownload.fetch(
            update.apk.url,
            File(dir, update.apk.name),
            update.apk.sha256,
            update.apk.size,
            onProgress
        )
    }

    fun clearCache() {
        runCatching { File(context.cacheDir, "update").deleteRecursively() }
    }

    private fun File.matches(asset: UpdateAsset): Boolean {
        if (length() != asset.size) return false
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
            .equals(asset.sha256, ignoreCase = true)
    }

    private fun mb(bytes: Long): String = "%.1f MB".format(bytes / 1024.0 / 1024.0)

    companion object {
        const val OWNER = "nurfilardi"
        const val REPO = "ytcutter"
    }
}