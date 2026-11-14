package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val downloadId: String,
    val songId: String,
    val title: String,
    val downloadType: String, // "FULL_VIDEO" or "AUDIO_ONLY"
    val fileSizeFormatted: String,
    val downloadedAt: Long,
    val durationFormatted: String,
    val localFilePath: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val songId: String? = null,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val isNewRelease: Boolean = true,
    val thumbnailUrl: String? = null,
    val linkedUrl: String? = null
)

@Entity(tableName = "local_files")
data class LocalFileEntity(
    @PrimaryKey val id: String,
    val uriString: String,
    val displayName: String,
    val sizeFormatted: String,
    val mimeType: String,
    val durationFormatted: String,
    val mediaType: String = "AUDIO", // VIDEO, AUDIO, IMAGE
    val folderName: String = "موسيقى جهازي",
    val sizeBytes: Long = 0L,
    val dateModified: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val coverColorHex: Long = 0xFF990000,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "songId"])
data class PlaylistItemEntity(
    val playlistId: String,
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_played")
data class RecentPlayedEntity(
    @PrimaryKey val songId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "imported_media")
data class ImportedMediaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val thumbnailUrl: String?,
    val platform: String,
    val videoId: String,
    val isShort: Boolean = false,
    val description: String = "",
    val channelName: String = "",
    val channelHandle: String = "",
    val categoryName: String = "IMPORTED",
    val mediaType: String = "ALL",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_channel_media")
data class CachedChannelMediaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoId: String,
    val sourceUrl: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val viewsCount: String,
    val description: String,
    val isShort: Boolean,
    val categoryName: String,
    val channelName: String = "Rahma Riad | رحمة رياض",
    val channelHandle: String = "@RahmaRiad",
    val isOfficialChannel: Boolean = true,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reel_comments")
data class ReelCommentEntity(
    @PrimaryKey val id: String,
    val reelId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reel_likes")
data class ReelLikeEntity(
    @PrimaryKey val reelId: String,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "featured_preferences")
data class FeaturedPreferenceEntity(
    @PrimaryKey val songId: String,
    val isFeatured: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
