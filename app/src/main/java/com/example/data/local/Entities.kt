package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val downloadId: String,
    val songId: String,
    val title: String,
    val downloadType: String, // "FULL_VIDEO" or "AUDIO_ONLY"
    val fileSizeFormatted: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val durationFormatted: String,
    val localFilePath: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val songId: String?,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val isNewRelease: Boolean = true
)

@Entity(tableName = "local_files")
data class LocalFileEntity(
    @PrimaryKey
    val id: String,
    val uriString: String,
    val displayName: String,
    val sizeFormatted: String,
    val mimeType: String,
    val durationFormatted: String = "ملف محلي",
    val addedAt: Long = System.currentTimeMillis()
)

