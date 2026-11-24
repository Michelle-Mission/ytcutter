package app.ytcutter.core.data

import android.content.Context
import app.ytcutter.core.model.ComponentInfo
import app.ytcutter.core.model.ReleaseInfo
import app.ytcutter.core.data.update.AppUpdater

class ComponentRepository(
    private val context: Context,
    private val appVersion: String
) {

    suspend fun components(): List<ComponentInfo> {
        val ytDlp = runCatching { YtDlpRepo.versionName(context) }.getOrNull()
        val app = runCatching { AppUpdater(context).check() }.getOrNull()
        return listOf(
            ComponentInfo(
                name = "Aplikasi YT Cutter",
                version = appVersion,
                updatable = true,
                note = if (app != null) {
                    "Build ${app.versionCode} siap dipasang"
                } else {
                    "Tidak ada rilis lebih baru"
                }
            ),
            ComponentInfo(
                name = "yt-dlp",
                version = ytDlp ?: "belum siap",
                updatable = true,
                note = "Diperbarui dari rilis resmi yt-dlp"
            ),
            ComponentInfo(
                name = "NewPipe Extractor",
                version = NEWPIPE_VERSION,
                updatable = false,
                note = "Tertanam di dalam aplikasi"
            )
        )
    }

    suspend fun updateYtDlp(): String = YtDlpRepo.update(context)

    /** Halaman rilis aplikasi terbaru, atau null bila sudah paling baru/gagal. */
    suspend fun latestRelease(): ReleaseInfo? = runCatching {
        val latest = AppUpdater(context).check() ?: return@runCatching null
        ReleaseInfo(
            version = latest.versionName,
            tag = "build-${latest.versionCode}",
            url = "https://github.com/${AppUpdater.OWNER}/${AppUpdater.REPO}/releases/latest",
            notes = latest.notes
        )
    }.getOrNull()

    private companion object {
        const val NEWPIPE_VERSION = "v0.27.1"
    }
}