package app.ytcutter.core.model

data class Maintenance(val enabled: Boolean, val message: String)

data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val url: String?
)

data class FeatureFlags(val downloader: Boolean = true, val player: Boolean = true)

data class AppConfig(
    val schemaVersion: Int = 1,
    val maintenance: Maintenance = Maintenance(false, ""),
    val announcement: Announcement? = null,
    val features: FeatureFlags = FeatureFlags(),
    val minVersionCode: Long = 0
) {
    companion object {
        val DEFAULT = AppConfig()
    }
}

data class ChangeEntry(val label: String, val items: List<String>)
