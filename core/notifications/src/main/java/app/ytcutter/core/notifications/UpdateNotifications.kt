package app.ytcutter.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notifikasi update aplikasi. Kanal dipakai ulang oleh notifikasi progres unduhan
 * lewat id terpisah sehingga tidak saling menimpa.
 */
object UpdateNotifications {

    private const val CHANNEL_ID = "updates"
    private const val NOTIFICATION_ID = 4101

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pembaruan",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Pemberitahuan versi baru YT Cutter" }
        manager.createNotificationChannel(channel)
    }

    fun notifyAvailable(context: Context, version: String, url: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            version.hashCode(),
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("YT Cutter $version tersedia")
            .setContentText("Ketuk untuk membuka halaman rilis.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}