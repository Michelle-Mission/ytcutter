package app.ytcutter.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object MediaStorePublisher {

    fun publish(context: Context, temp: File, displayName: String, audio: Boolean): Uri? {
        val collection = if (audio) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val relativePath = if (audio) {
            "${Environment.DIRECTORY_MUSIC}/YT Cutter"
        } else {
            "${Environment.DIRECTORY_MOVIES}/YT Cutter"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, if (audio) "audio/mp4" else "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                temp.inputStream().use { input -> input.copyTo(out) }
            } ?: throw IllegalStateException("tidak bisa membuka output MediaStore")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            val updated = resolver.update(uri, done, null, null)
            if (updated != 1) {
                resolver.delete(uri, null, null)
                return null
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }
}