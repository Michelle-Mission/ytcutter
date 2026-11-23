package app.ytcutter.core.data

import android.content.Context
import app.ytcutter.core.model.ComponentInfo
import app.ytcutter.core.network.GitHubReleasesApi

class ComponentRepository(
    private val context: Context,
    private val appVersion: String,
    private val releaseApi: GitHubReleasesApi
) {

    suspend fun components(): List<ComponentInfo> {
        val ytDlp = runCatching { YtDlpRepo.versionName(context) }.getOrNull()
        val app = runCatching { releaseApi.latest() }.getOrNull()
        return listOf(
            ComponentInfo(
                name = "Aplikasi YT Cutter",
                version = appVersion,
                updatable = true,
                note = if (app != null && app.version != appVersion) {
                    "Rilis terbaru ${app.version}"
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
    suspend fun latestRelease(): app.ytcutter.core.model.ReleaseInfo? = runCatching {
        val latest = releaseApi.latest() ?: return@runCatching null
        val current = app.ytcutter.core.common.Version.parse(appVersion) ?: return@runCatching null
        val newer = app.ytcutter.core.common.Version.parse(latest.version) ?: return@runCatching null
        if (newer > current) latest else null
    }.getOrNull()

    private companion object {
        const val NEWPIPE_VERSION = "v0.27.1"
    }
}