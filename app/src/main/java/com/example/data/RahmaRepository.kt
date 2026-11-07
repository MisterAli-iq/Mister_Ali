package com.example.data

import android.content.Context
import com.example.data.local.AppDao
import com.example.data.local.DownloadEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.LocalFileEntity
import com.example.data.local.NotificationEntity
import com.example.model.AppNotification
import com.example.model.DownloadStatus
import com.example.model.DownloadType
import com.example.model.DownloadedMedia
import com.example.model.Song
import com.example.model.SongCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RahmaRepository(
    private val context: Context,
    private val dao: AppDao
) {
    fun getAllSongs(): List<Song> = RahmaCatalog.songs

    fun getSongById(id: String): Song? = RahmaCatalog.getSongById(id)

    fun filterByCategory(category: SongCategory): List<Song> = RahmaCatalog.filterByCategory(category)

    fun searchSongs(query: String): List<Song> = RahmaCatalog.searchSongs(query)

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

    // Downloads
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

    fun isSongDownloaded(songId: String): Flow<Boolean> = dao.isSongDownloaded(songId)

    suspend fun deleteDownload(downloadId: String, localFilePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore file deletion error
            }
            dao.deleteDownload(downloadId)
        }
    }

    /**
     * Executes download of video or audio with simulated realistic stream download
     * and saves local cached file to internal storage.
     */
    fun startDownload(song: Song, type: DownloadType): Flow<Int> = flow {
        emit(5)
        delay(300)
        emit(25)
        delay(400)
        emit(55)
        delay(400)
        emit(85)
        delay(300)

        // Create local cached file in app internal cache/files directory
        val ext = type.extension
        val fileName = "rahma_${song.id}_${type.name.lowercase()}.$ext"
        val storageDir = File(context.filesDir, "rahma_downloads").apply { if (!exists()) mkdirs() }
        val targetFile = File(storageDir, fileName)
        
        // Write mock media placeholder file content
        val estimatedMb = if (type == DownloadType.FULL_VIDEO) (song.durationSeconds * 0.12).coerceAtLeast(14.0) else (song.durationSeconds * 0.035).coerceAtLeast(3.8)
        val sizeFormatted = String.format("%.1f MB", estimatedMb)
        
        withContext(Dispatchers.IO) {
            targetFile.writeText("RAHMA_RIAD_MEDIA_OFFLINE_CACHE:${song.id}:${type.name}")
            val entity = DownloadEntity(
                downloadId = UUID.randomUUID().toString(),
                songId = song.id,
                title = song.arabicTitle,
                downloadType = type.name,
                fileSizeFormatted = sizeFormatted,
                downloadedAt = System.currentTimeMillis(),
                durationFormatted = song.durationFormatted,
                localFilePath = targetFile.absolutePath
            )
            dao.saveDownload(entity)
        }

        emit(100)
    }.flowOn(Dispatchers.IO)

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
                    isNewRelease = it.isNewRelease
                )
            }
        }
    }

    fun getUnreadCount(): Flow<Int> = dao.getUnreadNotificationCount()

    suspend fun markAsRead(id: String) {
        withContext(Dispatchers.IO) {
            dao.markNotificationAsRead(id)
        }
    }

    suspend fun seedInitialNotificationsIfNeeded() {
        withContext(Dispatchers.IO) {
            val initialNotifications = listOf(
                NotificationEntity(
                    id = "notif_sabaa",
                    songId = "sabaa_alsaada",
                    title = "🔥 جديد الفنانة رحمة رياض: 'سبع السعادة'",
                    message = "تم إصدار الأغنية والفيديو كليب الجديد رسمياً! شاهده واستمع إليه الآن حصرياً وبأعلى جودة.",
                    timestamp = "منذ ساعتين",
                    isRead = false,
                    isNewRelease = true
                ),
                NotificationEntity(
                    id = "notif_asaad",
                    songId = "asaad_lel_gomar",
                    title = "✨ عمل غنائي جديد: 'أصعد للكمر'",
                    message = "كليب 'أصعد للكمر' يتصدر قائمة الأكثر رواجاً واهتماماً على يوتيوب ومنصات الموسيقى العربية.",
                    timestamp = "أمس",
                    isRead = false,
                    isNewRelease = true
                ),
                NotificationEntity(
                    id = "notif_welcome",
                    songId = "alkawkab",
                    title = "🌟 أهلاً بك في تطبيق RahmaRiad",
                    message = "استمتع بجميع أعمال رحمة رياض بدون إنترنت، وشارك الأغاني مع أصدقائك. تطوير ✪Mister Ali",
                    timestamp = "مرحباً بك",
                    isRead = true,
                    isNewRelease = false
                )
            )
            for (notif in initialNotifications) {
                dao.insertNotification(notif)
            }
        }
    }

    // Local Device Files
    fun getAllLocalFiles(): Flow<List<LocalFileEntity>> {
        return dao.getAllLocalFiles()
    }

    suspend fun saveLocalFiles(files: List<LocalFileEntity>) = withContext(Dispatchers.IO) {
        dao.insertLocalFiles(files)
    }

    suspend fun deleteLocalFile(id: String) = withContext(Dispatchers.IO) {
        dao.deleteLocalFile(id)
    }

    suspend fun clearLocalFiles() = withContext(Dispatchers.IO) {
        dao.clearLocalFiles()
    }
}
