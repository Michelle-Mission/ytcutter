package app.ytcutter.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import app.ytcutter.service.DownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val title: String, val progress: Float, val detail: String) : DownloadState
    data class Done(val title: String, val uri: Uri) : DownloadState
    data class Failed(val message: String) : DownloadState
    data object Canceled : DownloadState
}

object Downloads {

    const val EXTRA_URL = "url"
    const val EXTRA_HEIGHT = "height"
    const val EXTRA_LABEL = "label"
    const val EXTRA_START = "start"
    const val EXTRA_END = "end"
    const val EXTRA_TITLE = "title"

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    @Volatile
    var cancelRequested: Boolean = false
        private set

    fun start(context: Context, request: DownloadRequest, title: String) {
        cancelRequested = false
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra(EXTRA_URL, request.url)
            putExtra(EXTRA_HEIGHT, request.quality.height ?: -1)
            putExtra(EXTRA_LABEL, request.quality.label)
            putExtra(EXTRA_START, request.start)
            putExtra(EXTRA_END, request.end)
            putExtra(EXTRA_TITLE, title)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancel() {
        cancelRequested = true
        YtDlpRepo.cancel()
        _state.value = DownloadState.Canceled
    }

    internal fun update(state: DownloadState) {
        _state.value = state
    }
}