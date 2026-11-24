package app.ytcutter.core.model

data class UpdateAsset(
    val name: String,
    val url: String,
    val sha256: String,
    val size: Long
)

data class AppUpdate(
    val versionName: String,
    val versionCode: Long,
    val notes: String,
    val apk: UpdateAsset
) {
    fun isNewerThan(installedVersionCode: Long): Boolean = versionCode > installedVersionCode
}
