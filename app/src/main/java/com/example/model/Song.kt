package com.example.model

enum class SongCategory(val displayName: String) {
    ALL("الكل"),
    FEATURED("أغاني مميزة"),
    LATEST("أحدث الأعمال"),
    CONCERTS("حفلات ومهرجانات"),
    MUSIC_VIDEO("فيديو كليب"),
    REELS("ريلز وشورتس"),
    IMPORTED("روابطي المستوردة");

    val titleArabic: String
        get() = displayName

    companion object {
        fun fromStoredName(raw: String?): SongCategory {
            if (raw.isNullOrBlank()) return LATEST
            if (raw.equals("CLIPS", ignoreCase = true)) return MUSIC_VIDEO
            return runCatching { valueOf(raw.trim()) }.getOrDefault(LATEST)
        }
    }
}

enum class DownloadType(val titleArabic: String) {
    FULL_VIDEO("فيديو كامل"),
    AUDIO_ONLY("صوت فقط")
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

data class Song(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val category: SongCategory = SongCategory.ALL,
    val durationSeconds: Int = 0,
    val durationFormatted: String = "",
    val viewsCount: String = "",
    val likesCount: String = "",
    val releaseYear: String = "",
    val description: String = "",
    val lyrics: String = "",
    val composer: String = "رحمة رياض",
    val lyricist: String = "",
    val isNewRelease: Boolean = false,
    val isFeatured: Boolean = false,
    val isVideoAvailable: Boolean = true,
    val youtubeVideoId: String = "",
    val thumbnailUrl: String? = null,
    val sourcePlatform: String = "Rahma Riad | رحمة رياض",
    val publishDate: String = "",
    val externalUrl: String? = null,
    val resolvedStreamUrl: String? = null,
    val localFilePath: String? = null,
    val isShort: Boolean = false,
    val isUserImported: Boolean = false,
    val channelHandle: String = "@RahmaRiad",
    val mediaType: String = "VIDEO",
    val album: String = "سنجل",
    val gradientStartHex: Long = 0xFF2B0810L,
    val gradientEndHex: Long = 0xFF0F0F0FL
) {
    val isShortReel: Boolean
        get() = isShort
}

data class RahmaReel(
    val id: String,
    val title: String,
    val caption: String = "",
    val sourceUrl: String = "",
    val youtubeVideoId: String = "",
    val thumbnailUrl: String? = null,
    val resolvedStreamUrl: String? = null,
    val platformLabel: String = "رحمة رياض الرسمية",
    val songTitle: String = "",
    val dateText: String = "",
    val initialLikes: Int = 0,
    val isUserImported: Boolean = false,
    val tags: List<String> = emptyList(),
    val channelHandle: String = "@RahmaRiad",
    val gradientStartHex: Long = 0xFF2B0810L,
    val gradientEndHex: Long = 0xFF0F0F0FL
)

enum class ReelFilter(val displayName: String) {
    ALL("الكل"),
    OFFICIAL("الرسمية"),
    INSTAGRAM("إنستغرام"),
    TIKTOK("تيك توك"),
    IMPORTED("المستوردة"),
    SAVED("المحفوظة")
}

data class ReelComment(
    val id: String,
    val reelId: String,
    val authorName: String,
    val text: String,
    val timeAgo: String = "الآن"
)

data class AppNotification(
    val id: String,
    val songId: String? = null,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val isNewRelease: Boolean = true,
    val thumbnailUrl: String? = null,
    val linkedUrl: String? = null
)

data class AppUser(
    val name: String,
    val email: String,
    val isGuest: Boolean = false
)
