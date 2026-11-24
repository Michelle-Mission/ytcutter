package app.ytcutter.core.data.update

import android.content.Context
import android.content.pm.PackageManager
import app.ytcutter.core.model.AppUpdate
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

    suspend fun prepare(
        update: AppUpdate,
        onStatus: (String) -> Unit = {},
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "update").apply {
            deleteRecursively()
            mkdirs()
        }
        onStatus("Mengunduh APK ${mb(update.apk.size)}")
        val full = HttpDownload.fetch(
            update.apk.url,
            File(dir, update.apk.name),
            update.apk.sha256,
            update.apk.size,
            onProgress
        )
        full.validateForInstall()
        full
    }

    /** Android menolak APK rusak atau bertanda tangan berbeda ("paket tidak valid"). */
    private fun File.validateForInstall() {
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archive = pm.getPackageArchiveInfo(absolutePath, flags)
            ?: error("APK tidak valid")
        require(archive.packageName == context.packageName) { "Paket APK tidak cocok" }
        val installed = pm.getPackageInfo(context.packageName, flags)
        fun signers(info: android.content.pm.PackageInfo): Set<String> =
            info.signingInfo?.apkContentsSigners
                ?.map { it.toByteArray().sha256() }
                ?.toSet()
                .orEmpty()
        val expected = signers(installed)
        require(expected.isNotEmpty() && signers(archive) == expected) {
            "Tanda tangan APK berbeda"
        }
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

    fun clearCache() {
        runCatching { File(context.cacheDir, "update").deleteRecursively() }
    }

    private fun mb(bytes: Long): String = "%.1f MB".format(bytes / 1024.0 / 1024.0)

    companion object {
        const val OWNER = "nurfilardi"
        const val REPO = "ytcutter"
    }
}