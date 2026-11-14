package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.example.model.LocalMediaSortOption
import com.example.model.LocalMediaType

object LocalMediaScanner {

    private const val TAG = "LocalMediaScanner"

    /**
     * Play-Policy-compliant device media scanner:
     * 1. Recursively scans all user-granted SAF folder trees (videos, audio, images)
     * 2. Merges accessible MediaStore Audio/Video entries without requesting restricted permissions
     */
    fun scanAllDeviceMedia(
        context: Context,
        grantedFolderUris: Set<String> = emptySet()
    ): List<LocalFileEntity> {
        val dedupedByUri = linkedMapOf<String, LocalFileEntity>()

        // 1. Scan all persisted SAF document trees (Videos, Audio, Images + Folder name + Modified Date)
        grantedFolderUris.forEach { treeUriStr ->
            val treeUri = runCatching { Uri.parse(treeUriStr) }.getOrNull() ?: return@forEach
            val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
                ?: return@forEach
            val rootFolderName = extractFolderDisplayName(context, treeUri, rootDocId)
            scanDocumentTreeRecursive(
                context = context,
                treeUri = treeUri,
                parentDocId = rootDocId,
                folderLabel = rootFolderName,
                depth = 0,
                output = dedupedByUri
            )
        }

        // 2. Scan accessible Audio entries from MediaStore
        scanMediaStoreAudio(context, dedupedByUri)

        // 3. Scan accessible Video entries from MediaStore
        scanMediaStoreVideo(context, dedupedByUri)

        // 4. Scan accessible Image entries from MediaStore
        scanMediaStoreImages(context, dedupedByUri)

        return dedupedByUri.values.sortedByDescending { it.dateModified }
    }

    private fun extractFolderDisplayName(context: Context, treeUri: Uri, docId: String): String {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        runCatching {
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        return docId.substringAfterLast(':').substringAfterLast('/').ifBlank { "مجلد وسائط" }
    }

    private fun scanDocumentTreeRecursive(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        folderLabel: String,
        depth: Int,
        output: MutableMap<String, LocalFileEntity>
    ) {
        if (depth > 4) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx != -1) cursor.getString(idIdx) ?: continue else continue
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "ملف" else "ملف"
                    val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: "" else ""
                    val size = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L
                    val modified = if (modIdx != -1) cursor.getLong(modIdx) else System.currentTimeMillis()

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (!name.startsWith(".")) {
                            scanDocumentTreeRecursive(
                                context = context,
                                treeUri = treeUri,
                                parentDocId = docId,
                                folderLabel = name,
                                depth = depth + 1,
                                output = output
                            )
                        }
                    } else {
                        val detectedType = classifyMediaType(mime, name) ?: continue
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                        output[fileUri] = LocalFileEntity(
                            id = "saf_${fileUri.hashCode()}",
                            uriString = fileUri,
                            displayName = name,
                            sizeFormatted = formatFileSize(size),
                            mimeType = mime.ifBlank { fallbackMimeForType(detectedType) },
                            durationFormatted = when (detectedType) {
                                "IMAGE" -> "صورة"
                                "VIDEO" -> "فيديو"
                                else -> "صوت"
                            },
                            mediaType = detectedType,
                            folderName = folderLabel,
                            sizeBytes = size,
                            dateModified = if (modified > 0) modified else System.currentTimeMillis(),
                            addedAt = System.currentTimeMillis()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SAF tree scan skipped ($folderLabel): ${e.message}")
        }
    }

    private fun scanMediaStoreAudio(
        context: Context,
        output: MutableMap<String, LocalFileEntity>
    ) {
        try {
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
            )
            context.contentResolver.query(
                uri,
                projection,
                "${MediaStore.Audio.Media.SIZE} > 0",
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    if (idCol == -1) continue
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id).toString()
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "ملف صوتي" else "ملف صوتي"
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"
                    val durationMs = if (durCol != -1) cursor.getLong(durCol) else 0L
                    val dateSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                    val folder = if (bucketCol != -1) cursor.getString(bucketCol).orEmpty().ifBlank { "الموسيقى" } else "الموسيقى"

                    output.putIfAbsent(
                        contentUri,
                        LocalFileEntity(
                            id = "audio_$id",
                            uriString = contentUri,
                            displayName = name,
                            sizeFormatted = formatFileSize(size),
                            mimeType = mime,
                            durationFormatted = formatDuration(durationMs),
                            mediaType = "AUDIO",
                            folderName = folder,
                            sizeBytes = size,
                            dateModified = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio MediaStore query note: ${e.message}")
        }
    }

    private fun scanMediaStoreVideo(
        context: Context,
        output: MutableMap<String, LocalFileEntity>
    ) {
        try {
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )
            context.contentResolver.query(
                uri,
                projection,
                "${MediaStore.Video.Media.SIZE} > 0",
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    if (idCol == -1) continue
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id).toString()
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "مقطع فيديو" else "مقطع فيديو"
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                    val durationMs = if (durCol != -1) cursor.getLong(durCol) else 0L
                    val dateSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                    val folder = if (bucketCol != -1) cursor.getString(bucketCol).orEmpty().ifBlank { "الفيديوهات" } else "الفيديوهات"

                    output.putIfAbsent(
                        contentUri,
                        LocalFileEntity(
                            id = "video_$id",
                            uriString = contentUri,
                            displayName = name,
                            sizeFormatted = formatFileSize(size),
                            mimeType = mime,
                            durationFormatted = formatDuration(durationMs),
                            mediaType = "VIDEO",
                            folderName = folder,
                            sizeBytes = size,
                            dateModified = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Video MediaStore query note: ${e.message}")
        }
    }

    private fun scanMediaStoreImages(
        context: Context,
        output: MutableMap<String, LocalFileEntity>
    ) {
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            context.contentResolver.query(
                uri,
                projection,
                "${MediaStore.Images.Media.SIZE} > 0",
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    if (idCol == -1) continue
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id).toString()
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "صورة" else "صورة"
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val dateSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                    val folder = if (bucketCol != -1) cursor.getString(bucketCol).orEmpty().ifBlank { "الصور" } else "الصور"

                    output.putIfAbsent(
                        contentUri,
                        LocalFileEntity(
                            id = "image_$id",
                            uriString = contentUri,
                            displayName = name,
                            sizeFormatted = formatFileSize(size),
                            mimeType = mime,
                            durationFormatted = "صورة",
                            mediaType = "IMAGE",
                            folderName = folder,
                            sizeBytes = size,
                            dateModified = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Images MediaStore query note: ${e.message}")
        }
    }

    fun filterAndSortLocalMedia(
        items: List<LocalFileEntity>,
        query: String,
        typeFilter: LocalMediaType,
        sortOption: LocalMediaSortOption
    ): List<LocalFileEntity> {
        val trimmed = query.trim()
        val filtered = items.filter { item ->
            val resolvedType = resolveEntityMediaType(item)
            val matchesType = when (typeFilter) {
                LocalMediaType.ALL -> true
                LocalMediaType.VIDEO -> resolvedType == "VIDEO"
                LocalMediaType.AUDIO -> resolvedType == "AUDIO"
                LocalMediaType.IMAGE -> resolvedType == "IMAGE"
            }
            val matchesQuery = trimmed.isBlank() ||
                item.displayName.contains(trimmed, ignoreCase = true) ||
                item.folderName.contains(trimmed, ignoreCase = true) ||
                item.mimeType.contains(trimmed, ignoreCase = true)
            matchesType && matchesQuery
        }

        return when (sortOption) {
            LocalMediaSortOption.DATE_DESC -> filtered.sortedByDescending { maxOf(it.dateModified, it.addedAt) }
            LocalMediaSortOption.NAME_ASC -> filtered.sortedWith(
                compareBy<LocalFileEntity> { it.displayName.lowercase() }
                    .thenByDescending { maxOf(it.dateModified, it.addedAt) }
            )
            LocalMediaSortOption.FOLDER -> filtered.sortedWith(
                compareBy<LocalFileEntity> { it.folderName.lowercase() }
                    .thenByDescending { maxOf(it.dateModified, it.addedAt) }
            )
        }
    }

    fun resolveEntityMediaType(item: LocalFileEntity): String {
        if (item.mediaType in setOf("VIDEO", "AUDIO", "IMAGE")) return item.mediaType
        return classifyMediaType(item.mimeType, item.displayName) ?: "AUDIO"
    }

    fun classifyMediaType(mimeType: String, fileName: String): String? {
        val lowerMime = mimeType.lowercase()
        val lowerName = fileName.lowercase()
        return when {
            lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                lowerName.endsWith(".webm") || lowerName.endsWith(".mov") || lowerName.endsWith(".avi") -> "VIDEO"
            lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") ||
                lowerName.endsWith(".wav") || lowerName.endsWith(".aac") || lowerName.endsWith(".ogg") ||
                lowerName.endsWith(".flac") -> "AUDIO"
            lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") -> "IMAGE"
            else -> null
        }
    }

    private fun fallbackMimeForType(mediaType: String): String = when (mediaType) {
        "VIDEO" -> "video/mp4"
        "IMAGE" -> "image/jpeg"
        else -> "audio/mpeg"
    }

    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "ملف محلي"
        val totalSecs = durationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d", mins, secs)
    }
}
