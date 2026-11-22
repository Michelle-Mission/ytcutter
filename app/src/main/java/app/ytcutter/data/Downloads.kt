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
    data class Failed(
        val message: String,
        val action: Action = Action.NONE
    ) : DownloadState
    data object Canceled : DownloadState
}

object Downloads {

    const val EXTRA_URL = "url"
    const val EXTRA_HEIGHT = "height"
    const val EXTRA_LABEL = "label"
    const val EXTRA_START = "start"
    const val EXTRA_END = "end"
    const val EXTRA_TITLE = "title"
    const val EXTRA_PROCESS_ID = "process_id"
    const val EXTRA_INFO_JSON = "info_json"

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    @Volatile
    var cancelRequested: Boolean = false
        private set

    @Volatile
    private var currentProcessId: String? = null

    @Volatile
    private var lastRequest: DownloadRequest? = null

    @Volatile
    private var lastTitle: String = ""

    fun isRunning(): Boolean = _state.value is DownloadState.Running

    fun retry(context: Context): Boolean {
        val request = lastRequest ?: return false
        return start(context, request, lastTitle)
    }

    fun start(context: Context, request: DownloadRequest, title: String): Boolean {
        if (isRunning()) return false
        cancelRequested = false
        lastRequest = request
        lastTitle = title
        val processId = YtDlpRepo.newProcessId()
        currentProcessId = processId
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra(EXTRA_URL, request.url)
            putExtra(EXTRA_HEIGHT, request.quality.height ?: -1)
            putExtra(EXTRA_LABEL, request.quality.label)
            putExtra(EXTRA_START, request.start)
            putExtra(EXTRA_END, request.end)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_PROCESS_ID, processId)
            putExtra(EXTRA_INFO_JSON, request.infoJson)
        }
        ContextCompat.startForegroundService(context, intent)
        return true
    }

    fun cancel() {
        cancelRequested = true
        YtDlpRepo.cancel(currentProcessId)
        _state.value = DownloadState.Canceled
    }

    internal fun finished() {
        currentProcessId = null
    }

    internal fun update(state: DownloadState) {
        _state.value = state
    }
}