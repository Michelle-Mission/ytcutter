package app.ytcutter.core.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

sealed class InstallResult {
    data object Installed : InstallResult()
    data object Cancelled : InstallResult()
    data object NeedsPermission : InstallResult()
    data class Failed(val message: String) : InstallResult()
}

object ApkInstaller {

    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openPermissionSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    suspend fun install(context: Context, apk: File): InstallResult {
        if (!canInstall(context)) return InstallResult.NeedsPermission
        return withTimeoutOrNull(180_000) {
            suspendCancellableCoroutine { cont ->
                val action = "${context.packageName}.INSTALL_RESULT_${System.currentTimeMillis()}"
                lateinit var receiver: BroadcastReceiver

                fun finish(result: InstallResult) {
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) cont.resume(result)
                }

                receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        when (val status = intent.getIntExtra(
                            PackageInstaller.EXTRA_STATUS,
                            PackageInstaller.STATUS_FAILURE
                        )) {
                            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                                @Suppress("DEPRECATION")
                                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (confirm != null) {
                                    runCatching { context.startActivity(confirm) }
                                        .onFailure { finish(InstallResult.Failed("Tidak bisa membuka dialog pasang")) }
                                }
                            }
                            PackageInstaller.STATUS_SUCCESS -> finish(InstallResult.Installed)
                            PackageInstaller.STATUS_FAILURE_ABORTED,
                            PackageInstaller.STATUS_FAILURE_BLOCKED -> finish(InstallResult.Cancelled)
                            else -> finish(
                                InstallResult.Failed(
                                    intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                                        ?: "Gagal memasang (kode $status)"
                                )
                            )
                        }
                    }
                }

                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(action),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )

                runCatching {
                    val installer = context.packageManager.packageInstaller
                    val params = PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL
                    )
                    val sessionId = installer.createSession(params)
                    installer.openSession(sessionId).use { session ->
                        session.openWrite("base.apk", 0, apk.length()).use { out ->
                            apk.inputStream().use { it.copyTo(out) }
                            session.fsync(out)
                        }
                        val pending = android.app.PendingIntent.getBroadcast(
                            context,
                            sessionId,
                            Intent(action).setPackage(context.packageName),
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                android.app.PendingIntent.FLAG_MUTABLE
                        )
                        session.commit(pending.intentSender)
                    }
                }.onFailure {
                    finish(InstallResult.Failed(it.message ?: "Gagal memulai pemasangan"))
                }

                cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
            }
        } ?: InstallResult.Failed("Waktu pemasangan habis")
    }
}