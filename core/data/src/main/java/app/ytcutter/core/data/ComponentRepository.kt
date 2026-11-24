package app.ytcutter.core.data

import android.content.Context
import app.ytcutter.core.model.ComponentInfo
import app.ytcutter.core.model.ReleaseInfo
import app.ytcutter.core.data.update.AppUpdater
import app.ytcutter.core.data.engine.Engine

class ComponentRepository(
    private val context: Context,
    private val appVersion: String
) {

    suspend fun components(): List<ComponentInfo> {
        val ytDlp = runCatching { YtDlpRepo.versionName(context) }.getOrNull()
        val helper = runCatching { Engine.helperVersion(context) }.getOrNull()
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
                name = "Paket komponen",
                version = helper ?: "belum dipasang",
                updatable = true,
                note = if (helper != null) {
                    "FFmpeg, Python, dan aria2c aktif dari paket terpisah"
                } else {
                    "Pasang YT-Cutter-Components.apk dari halaman rilis agar FFmpeg, " +
                        "Python, dan aria2c bisa diperbarui tanpa memasang ulang aplikasi"
                }
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

}