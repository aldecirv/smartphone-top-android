package com.smartphonetop.app.core

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.smartphonetop.app.data.MediaEntry
import com.smartphonetop.app.data.MediaKind
import com.smartphonetop.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaScanner {

    suspend fun scan(context: Context): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (!Permissions.hasMediaPermission(context)) return@withContext emptyList()
        val result = mutableListOf<MediaEntry>()
        listOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE to MediaKind.IMAGE,
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO to MediaKind.VIDEO,
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO to MediaKind.AUDIO
        ).forEach { (type, kind) ->
            runCatching { queryType(context, type, kind) }.getOrDefault(emptyList()).let { result.addAll(it) }
        }
        result.distinctBy { it.uri }.sortedByDescending { it.sizeBytes }.take(500)
    }

    private fun queryType(context: Context, mediaType: Int, kind: MediaKind): List<MediaEntry> {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.MediaColumns.SIZE} > 0"
        val result = mutableListOf<MediaEntry>()
        context.contentResolver.query(
            collection, projection, selection, arrayOf(mediaType.toString()),
            "${MediaStore.MediaColumns.SIZE} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext() && result.size < 500) {
                result.add(
                    MediaEntry(
                        uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol)),
                        name = cursor.getString(nameCol) ?: "Arquivo",
                        sizeBytes = cursor.getLong(sizeCol),
                        dateAdded = cursor.getLong(dateCol),
                        mime = cursor.getString(mimeCol).orEmpty(),
                        kind = kind
                    )
                )
            }
        }
        return result
    }
}
