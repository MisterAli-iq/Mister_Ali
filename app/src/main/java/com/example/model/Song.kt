package com.example.model

enum class SongCategory(val displayName: String, val tag: String) {
    ALL("الكل", "all"),
    LATEST("أحدث الأغاني", "latest"),
    CLIPS("فيديو كليبات", "clips"),
    CONCERTS("حفلات مباشرة", "concerts"),
    SESSIONS("جلسات طربية", "sessions"),
    DUETS("ديو ومشاركات", "duets")
}

data class Song(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val category: SongCategory,
    val durationSeconds: Int,
    val durationFormatted: String,
    val viewsCount: String,
    val releaseYear: String,
    val description: String,
    val lyrics: String,
    val composer: String,
    val lyricist: String,
    val isNewRelease: Boolean = false,
    val gradientStartHex: Long = 0xFF8B0000,
    val gradientEndHex: Long = 0xFF1A1A1A,
    val isVideoAvailable: Boolean = true
)

enum class DownloadType(val titleArabic: String, val extension: String) {
    FULL_VIDEO("فيديو كامل بدقة عالية (MP4)", "mp4"),
    AUDIO_ONLY("مسار صوتي عالي النقاء (MP3)", "mp3")
}

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadedMedia(
    val id: String,
    val songId: String,
    val title: String,
    val downloadType: DownloadType,
    val fileSizeFormatted: String,
    val downloadedAt: Long,
    val durationFormatted: String,
    val localFilePath: String
)

data class AppNotification(
    val id: String,
    val songId: String?,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val isNewRelease: Boolean = true
)
