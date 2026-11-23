package app.ytcutter.core.model

data class Quality(val label: String, val height: Int?) {
    val isAudio: Boolean get() = height == null
}
