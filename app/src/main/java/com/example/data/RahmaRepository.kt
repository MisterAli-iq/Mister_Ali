package com.example.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.AppDao
import com.example.data.local.CachedChannelMediaEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.FeaturedPreferenceEntity
import com.example.data.local.ImportedMediaEntity
import com.example.data.local.LocalFileEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.local.RecentPlayedEntity
import com.example.data.local.ReelCommentEntity
import com.example.data.local.ReelLikeEntity
import com.example.data.remote.ChannelFetchPage
import com.example.data.remote.MediaStreamResolver
import com.example.data.remote.OfficialChannelFeedService
import com.example.model.AppNotification
import com.example.model.DownloadStorageLocation
import com.example.model.DownloadType
import com.example.model.DownloadedMedia
import com.example.model.PreferredQuality
import com.example.model.RahmaReel
import com.example.model.ReelComment
import com.example.model.Song
import com.example.model.SongCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class RahmaRepository(
    private val context: Context,
    private val dao: AppDao
) {
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Real Official Channel Media (Cached + Unlimited Pagination)
    fun getCachedChannelMedia(): Flow<List<Song>> {
        return dao.getAllCachedChannelMedia().map { list ->
            list.map { entity ->
                val cat = SongCategory.fromStoredName(entity.categoryName)
                val isOfficialIg = entity.channelHandle.equals(RahmaCatalog.OFFICIAL_INSTAGRAM_HANDLE, ignoreCase = true) ||
                    entity.sourceUrl.contains("instagram.com/rahmariadh", ignoreCase = true) ||
                    entity.id.startsWith("ig_")
                val isOfficial = entity.isOfficialChannel || isOfficialIg ||
                    RahmaCatalog.isOfficialRahmaChannelName(entity.channelName, entity.channelHandle)
                val realChannel = when {
                    isOfficialIg -> RahmaCatalog.OFFICIAL_INSTAGRAM_AR_NAME
                    isOfficial -> RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME
                    entity.channelName.isNotBlank() -> entity.channelName
                    else -> "يوتيوب"
                }
                val realHandle = when {
                    isOfficialIg -> RahmaCatalog.OFFICIAL_INSTAGRAM_HANDLE
                    isOfficial -> RahmaCatalog.OFFICIAL_CHANNEL_HANDLE
                    else -> entity.channelHandle
                }
                Song(
                    id = entity.id,
                    title = entity.title,
                    arabicTitle = entity.title,
                    category = cat,
                    durationSeconds = 0,
                    durationFormatted = if (entity.isShort) "ريلز" else "فيديو",
                    viewsCount = entity.viewsCount,
                    releaseYear = entity.publishedAt.take(4),
                    description = entity.description,
                    lyrics = entity.description,
                    composer = entity.channelName.ifBlank { "رحمة رياض" },
                    lyricist = when {
                        isOfficialIg -> "الحساب الرسمي @rahmariadh"
                        isOfficial -> "القناة الرسمية @RahmaRiad"
                        else -> realChannel
                    },
                    isNewRelease = false,
                    isVideoAvailable = true,
                    youtubeVideoId = entity.videoId,
                    thumbnailUrl = entity.thumbnailUrl,
                    sourcePlatform = realChannel,
                    publishDate = entity.publishedAt,
                    externalUrl = entity.sourceUrl,
                    isShort = entity.isShort,
                    isUserImported = false,
                    channelHandle = realHandle
                )
            }
        }
    }

    suspend fun syncOfficialChannelFeed(): List<CachedChannelMediaEntity> {
        return syncOfficialChannelPage(continuationToken = null).items
    }

    suspend fun syncOfficialChannelPage(continuationToken: String?): ChannelFetchPage {
        val page = OfficialChannelFeedService.fetchOfficialChannelMediaPage(continuationToken)
        if (page.items.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                dao.insertCachedChannelMedia(page.items)
                if (continuationToken == null) {
                    val latest = page.items.firstOrNull { it.isOfficialChannel } ?: page.items.first()
                    val senderChannel = if (latest.isOfficialChannel) "رحمة رياض الرسمية" else latest.channelName
                    dao.insertNotification(
                        NotificationEntity(
                            id = "notif_${latest.id}",
                            songId = latest.id,
                            title = "جديد $senderChannel: ${latest.title}",
                            message = "تم جلب أحدث إصدار من قناة $senderChannel (${latest.publishedAt})",
                            timestamp = latest.publishedAt,
                            isRead = false,
                            isNewRelease = true,
                            thumbnailUrl = latest.thumbnailUrl,
                            linkedUrl = latest.sourceUrl
                        )
                    )
                }
            }
        }
        return page
    }

    suspend fun smartSearchOnline(query: String): List<Song> {
        return OfficialChannelFeedService.smartSearchRahmaRiad(query)
    }

    // Real User-Imported Media & Reels
    fun getImportedSongs(): Flow<List<Song>> {
        return dao.getAllImportedMedia().map { list ->
            list.map { entity ->
                val resolvedChannel = entity.channelName.ifBlank { entity.platform }
                val resolvedCat = SongCategory.fromStoredName(entity.categoryName)
                Song(
                    id = entity.id,
                    title = entity.title,
                    arabicTitle = entity.title,
                    category = if (entity.isShort) SongCategory.REELS else resolvedCat,
                    durationSeconds = 0,
                    durationFormatted = if (entity.isShort) "ريلز" else "مباشر",
                    viewsCount = entity.platform,
                    releaseYear = "مستورد",
                    description = entity.description.ifBlank { "فيديو مستورد: ${entity.sourceUrl}" },
                    lyrics = entity.description.ifBlank { "المصدر: ${entity.sourceUrl}" },
                    composer = resolvedChannel,
                    lyricist = if (entity.channelHandle.isNotBlank()) entity.channelHandle else entity.platform,
                    isNewRelease = true,
                    isVideoAvailable = entity.mediaType != "AUDIO" && entity.mediaType != "IMAGE",
                    youtubeVideoId = entity.videoId,
                    thumbnailUrl = entity.thumbnailUrl,
                    sourcePlatform = resolvedChannel,
                    externalUrl = entity.sourceUrl,
                    isShort = entity.isShort,
                    isUserImported = true,
                    channelHandle = entity.channelHandle,
                    mediaType = entity.mediaType
                )
            }
        }
    }

    fun getImportedReels(): Flow<List<RahmaReel>> {
        return dao.getAllImportedMedia().map { list ->
            list.map { entity ->
                RahmaReel(
                    id = entity.id,
                    title = entity.title,
                    caption = entity.description.ifBlank { "مقطع مستورد من ${entity.platform}" },
                    sourceUrl = entity.sourceUrl,
                    youtubeVideoId = entity.videoId,
                    thumbnailUrl = entity.thumbnailUrl,
                    platformLabel = entity.platform,
                    songTitle = entity.title,
                    dateText = entity.platform,
                    isUserImported = true,
                    tags = listOf(entity.platform, "رحمة_رياض")
                )
            }
        }
    }

    suspend fun saveImportedMedia(item: ImportedMediaEntity) = withContext(Dispatchers.IO) {
        dao.insertImportedMedia(item)
    }

    suspend fun deleteImportedMedia(id: String) = withContext(Dispatchers.IO) {
        dao.deleteImportedMedia(id)
    }

    // Real Reel Comments
    fun getCommentsForReel(reelId: String): Flow<List<ReelComment>> {
        return dao.getCommentsForReel(reelId).map { list ->
            list.map { entity ->
                ReelComment(
                    id = entity.id,
                    reelId = entity.reelId,
                    authorName = entity.authorName,
                    text = entity.text,
                    timeAgo = "الآن"
                )
            }
        }
    }

    suspend fun addReelComment(reelId: String, authorName: String, text: String) = withContext(Dispatchers.IO) {
        dao.insertReelComment(
            ReelCommentEntity(
                id = "cmt_${UUID.randomUUID().toString().take(8)}",
                reelId = reelId,
                authorName = authorName,
                text = text
            )
        )
    }

    fun getLikedReelIds(): Flow<Set<String>> {
        return dao.getAllReelLikes().map { list -> list.map { it.reelId }.toSet() }
    }

    suspend fun toggleReelLike(reelId: String, isCurrentlyLiked: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentlyLiked) {
            dao.removeReelLike(reelId)
        } else {
            dao.insertReelLike(ReelLikeEntity(reelId = reelId))
        }
    }

    // Featured Song Preferences
    fun getFeaturedPreferences(): Flow<Map<String, Boolean>> {
        return dao.getAllFeaturedPreferences().map { list ->
            list.associate { it.songId to it.isFeatured }
        }
    }

    suspend fun setFeaturedPreference(songId: String, isFeatured: Boolean) {
        withContext(Dispatchers.IO) {
            dao.upsertFeaturedPreference(
                FeaturedPreferenceEntity(
                    songId = songId,
                    isFeatured = isFeatured,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Favorites
    fun getFavoriteSongIds(): Flow<Set<String>> {
        return dao.getAllFavorites().map { list ->
            list.map { it.songId }.toSet()
        }
    }

    suspend fun toggleFavorite(songId: String, isCurrentlyFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            if (isCurrentlyFavorite) {
                dao.removeFavorite(songId)
            } else {
                dao.addFavorite(FavoriteEntity(songId = songId))
            }
        }
    }

    // Real Stream Download Engine (Matched Headers + Quality + Audio/Video Separation + Device MediaStore Export)
    fun getAllDownloads(): Flow<List<DownloadedMedia>> {
        return dao.getAllDownloads().map { list ->
            list.map { entity ->
                DownloadedMedia(
                    id = entity.downloadId,
                    songId = entity.songId,
                    title = entity.title,
                    downloadType = if (entity.downloadType == "AUDIO_ONLY") DownloadType.AUDIO_ONLY else DownloadType.FULL_VIDEO,
                    fileSizeFormatted = entity.fileSizeFormatted,
                    downloadedAt = entity.downloadedAt,
                    durationFormatted = entity.durationFormatted,
                    localFilePath = entity.localFilePath
                )
            }
        }
    }

    suspend fun deleteDownload(downloadId: String, localFilePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            dao.deleteDownload(downloadId)
        }
    }

    suspend fun clearAllDownloads() {
        withContext(Dispatchers.IO) {
            val storageDir = File(context.filesDir, "rahma_downloads")
            if (storageDir.exists()) {
                storageDir.deleteRecursively()
            }
            storageDir.mkdirs()
            dao.clearAllDownloads()
        }
    }

    fun calculateDownloadsStorageFormatted(): String {
        val storageDir = File(context.filesDir, "rahma_downloads")
        val totalBytes = if (storageDir.exists()) {
            storageDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        val mb = totalBytes / (1024.0 * 1024.0)
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            totalBytes >= 1024L -> "${totalBytes / 1024} KB"
            else -> "0 MB"
        }
    }

    fun startDownload(
        song: Song,
        type: DownloadType,
        downloadQuality: PreferredQuality = PreferredQuality.HIGH,
        storageLocation: DownloadStorageLocation = DownloadStorageLocation.APP_AND_DEVICE,
        autoRetry: Boolean = true
    ): Flow<Int> = flow {
        emit(4)
        val sourceUrl = song.externalUrl ?: if (song.youtubeVideoId.isNotBlank()) {
            "https://www.youtube.com/watch?v=${song.youtubeVideoId}"
        } else ""

        val resolved = MediaStreamResolver.resolvePlayableStream(
            sourceUrl = sourceUrl,
            videoIdHint = song.youtubeVideoId,
            preferredQuality = downloadQuality
        )

        val candidateUrl = when (type) {
            DownloadType.AUDIO_ONLY -> resolved.downloadAudioUrl ?: resolved.downloadVideoUrl ?: resolved.streamUrl
            DownloadType.FULL_VIDEO -> resolved.downloadVideoUrl ?: resolved.streamUrl ?: song.resolvedStreamUrl
        } ?: throw IllegalStateException(resolved.errorMessage ?: "تعذر العثور على رابط مباشر متاح للتنزيل حالياً")

        if (candidateUrl.contains(".m3u8", ignoreCase = true)) {
            throw IllegalStateException("هذا المصدر يوفر بثاً تكيفيّاً للمشاهدة المباشرة فقط؛ جرّب تغييره إلى جودة قياسية بالإعدادات")
        }

        emit(12)
        val mimeHint = if (type == DownloadType.AUDIO_ONLY) resolved.audioMimeType else resolved.videoMimeType
        val ext = resolveFileExtension(type, mimeHint, candidateUrl)
        val safeTitle = song.arabicTitle
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(40)
            .ifBlank { song.id.replace(Regex("[^a-zA-Z0-9_-]"), "_") }
        val safeId = song.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "rahma_${safeId}_${type.name.lowercase()}.$ext"
        val storageDir = File(context.filesDir, "rahma_downloads").apply { if (!exists()) mkdirs() }
        val targetFile = File(storageDir, fileName)

        val maxAttempts = if (autoRetry) 3 else 1
        var downloadedBytes = 0L
        var lastError: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                val requestBuilder = Request.Builder().url(candidateUrl)
                if (resolved.requestHeaders.isNotEmpty()) {
                    resolved.requestHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }
                } else {
                    requestBuilder.header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    )
                }

                downloadClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Empty stream body")
                    val totalBytes = body.contentLength()
                    downloadedBytes = 0L
                    body.byteStream().use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            var lastEmitted = 12
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                if (totalBytes > 0) {
                                    val pct = (12 + (downloadedBytes * 78 / totalBytes)).toInt().coerceIn(12, 90)
                                    if (pct >= lastEmitted + 4) {
                                        lastEmitted = pct
                                        emit(pct)
                                    }
                                }
                            }
                        }
                    }
                }
                if (downloadedBytes > 1024) {
                    lastError = null
                    break
                } else {
                    throw IllegalStateException("حجم الملف الناتج غير مكتمل")
                }
            } catch (e: Exception) {
                lastError = e
                Log.w("RahmaDownload", "Attempt $attempt failed: ${e.message}")
                if (attempt < maxAttempts) {
                    delay(900L * attempt)
                }
            }
        }

        if (lastError != null) {
            if (targetFile.exists()) targetFile.delete()
            throw lastError
        }

        emit(94)

        // Export copy to public phone Downloads/Music/Movies via zero-permission MediaStore if enabled
        if (storageLocation == DownloadStorageLocation.APP_AND_DEVICE) {
            exportToDevicePublicStorage(
                sourceFile = targetFile,
                displayTitle = "${safeTitle}_${type.name.lowercase()}.$ext",
                mimeType = mimeHint.ifBlank { if (type == DownloadType.AUDIO_ONLY) "audio/mp4" else "video/mp4" },
                isAudio = type == DownloadType.AUDIO_ONLY
            )
        }

        val sizeMb = downloadedBytes / (1024.0 * 1024.0)
        val sizeFormatted = if (sizeMb >= 1.0) String.format("%.1f MB", sizeMb) else "${downloadedBytes / 1024} KB"

        dao.saveDownload(
            DownloadEntity(
                downloadId = UUID.randomUUID().toString(),
                songId = song.id,
                title = song.arabicTitle,
                downloadType = type.name,
                fileSizeFormatted = sizeFormatted,
                downloadedAt = System.currentTimeMillis(),
                durationFormatted = song.durationFormatted,
                localFilePath = targetFile.absolutePath
            )
        )
        emit(100)
    }.flowOn(Dispatchers.IO)

    private fun resolveFileExtension(type: DownloadType, mimeHint: String, url: String): String {
        val lowerMime = mimeHint.lowercase()
        val lowerUrl = url.lowercase()
        return when {
            type == DownloadType.FULL_VIDEO -> if (lowerMime.contains("webm") || lowerUrl.contains(".webm")) "webm" else "mp4"
            lowerMime.contains("mpeg") || lowerUrl.contains(".mp3") -> "mp3"
            lowerMime.contains("webm") -> "webm"
            else -> "m4a"
        }
    }

    private fun exportToDevicePublicStorage(
        sourceFile: File,
        displayTitle: String,
        mimeType: String,
        isAudio: Boolean
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val subDir = if (isAudio) "${Environment.DIRECTORY_DOWNLOADS}/RahmaRiad/Audio" else "${Environment.DIRECTORY_DOWNLOADS}/RahmaRiad/Video"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayTitle)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, subDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(collection, values) ?: return
                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            } else {
                val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (externalDir != null) {
                    if (!externalDir.exists()) externalDir.mkdirs()
                    val dest = File(externalDir, displayTitle)
                    sourceFile.copyTo(dest, overwrite = true)
                }
            }
        } catch (e: Exception) {
            Log.w("RahmaDownload", "MediaStore export skipped/failed: ${e.message}")
        }
    }

    // Notifications
    fun getNotifications(): Flow<List<AppNotification>> {
        return dao.getAllNotifications().map { list ->
            list.map {
                AppNotification(
                    id = it.id,
                    songId = it.songId,
                    title = it.title,
                    message = it.message,
                    timestamp = it.timestamp,
                    isRead = it.isRead,
                    isNewRelease = it.isNewRelease,
                    thumbnailUrl = it.thumbnailUrl,
                    linkedUrl = it.linkedUrl
                )
            }
        }
    }

    fun getUnreadCount(): Flow<Int> = dao.getUnreadNotificationCount()

    suspend fun markAsRead(id: String) = withContext(Dispatchers.IO) {
        dao.markNotificationAsRead(id)
    }

    // Local Device Files
    fun getAllLocalFiles(): Flow<List<LocalFileEntity>> = dao.getAllLocalFiles()

    suspend fun saveLocalFiles(files: List<LocalFileEntity>) = withContext(Dispatchers.IO) {
        dao.insertLocalFiles(files)
    }

    suspend fun deleteLocalFile(id: String) = withContext(Dispatchers.IO) {
        dao.deleteLocalFile(id)
    }

    suspend fun clearLocalFiles() = withContext(Dispatchers.IO) {
        dao.clearLocalFiles()
    }

    // Custom Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    suspend fun createPlaylist(name: String, description: String = "", colorHex: Long = 0xFF990000): String = withContext(Dispatchers.IO) {
        val id = "pl_${UUID.randomUUID().toString().take(8)}"
        dao.insertPlaylist(
            PlaylistEntity(
                id = id,
                name = name,
                description = description,
                coverColorHex = colorHex
            )
        )
        id
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    fun getAllPlaylistItems(): Flow<List<PlaylistItemEntity>> = dao.getAllPlaylistItems()

    suspend fun addSongToPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        dao.addSongToPlaylist(
            PlaylistItemEntity(playlistId = playlistId, songId = songId)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    // Recent History
    fun getRecentPlayed(): Flow<List<RecentPlayedEntity>> = dao.getRecentPlayed()

    suspend fun recordSongPlay(songId: String) = withContext(Dispatchers.IO) {
        dao.recordPlayedSong(
            RecentPlayedEntity(songId = songId, playedAt = System.currentTimeMillis())
        )
    }

    suspend fun clearRecentHistory() = withContext(Dispatchers.IO) {
        dao.clearRecentHistory()
    }
}
