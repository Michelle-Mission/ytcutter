package app.ytcutter.core.model

data class UpdateAsset(
    val name: String,
    val url: String,
    val sha256: String,
    val size: Long
)

data class UpdatePatch(
    val asset: UpdateAsset,
    val fromVersionCode: Long
)

data class AppUpdate(
    val versionName: String,
    val versionCode: Long,
    val notes: String,
    val apk: UpdateAsset,
    val patch: UpdatePatch?
) {
    fun isNewerThan(installedVersionCode: Long): Boolean = versionCode > installedVersionCode
}