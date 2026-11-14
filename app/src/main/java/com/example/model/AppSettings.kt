package com.example.model

enum class AppThemeMode(val titleArabic: String, val subtitleArabic: String) {
    YOUTUBE_DARK("الوضع الداكن الرسمي", "الخلفية الداكنة الكلاسيكية مع تباين مريح"),
    AMOLED_BLACK("أسود AMOLED فاحم", "مثالي لشاشات OLED وتوفير البطارية"),
    LIGHT("الوضع الفاتح", "إضاءة نهارية واضحة وألوان مشرقة"),
    ROYAL_GOLD("الذهبي الفاخر", "تصميم فخم بلمسات ذهبية دافئة")
}

enum class AppAccentColor(val titleArabic: String, val hex: Long) {
    RED("الأحمر الرسمي", 0xFFFF0033),
    GOLD("الذهبي العراقي", 0xFFFFC107),
    BLUE("الأزرق السماوي", 0xFF2196F3),
    EMERALD("الأخضر الزمردي", 0xFF00C853),
    PURPLE("البنفسجي الملكي", 0xFF9C27B0)
}

enum class PreferredQuality(val titleArabic: String, val maxHeightPx: Int) {
    AUTO("تلقائي حسب الشبكة", 1080),
    HIGH("1080p عالية الدقة", 1080),
    MEDIUM("720p متوسطة", 720),
    STANDARD_480("480p قياسية", 480),
    LOW_360("360p عادية", 360),
    DATA_SAVER("توفير البيانات (360p)", 360)
}

enum class DefaultDownloadFormat(val key: String, val titleArabic: String) {
    ASK("اسألني دائماً", "اسألني في كل مرة"),
    FULL_VIDEO("FULL_VIDEO", "فيديو كامل (MP4)"),
    AUDIO_ONLY("AUDIO_ONLY", "صوت فقط (M4A/MP3)")
}

enum class DownloadStorageLocation(val titleArabic: String, val subtitleArabic: String) {
    APP_AND_DEVICE("التطبيق وذاكرة الهاتف (معرض/موسيقى)", "حفظ داخل التطبيق + تصدير فوري لمجلد Downloads والموسيقى"),
    APP_ONLY("ذاكرة التطبيق فقط", "توفير المساحة مع تشغيل أوفلاين من داخل التطبيق")
}

enum class VideoAspectMode(val titleArabic: String) {
    FIT("ملاءمة كاملة (بدون قص)"),
    CROP("ملء الشاشة (قص تلقائي)")
}

enum class PlayerEngineMode(val titleArabic: String) {
    EXOPLAYER("المشغل الأصلي فائق السرعة"),
    WEB_ALTERNATIVE("المشغل البديل المدمج")
}

enum class LocalMediaType(val titleArabic: String) {
    ALL("الكل"),
    VIDEO("فيديو"),
    AUDIO("صوت"),
    IMAGE("صور")
}

enum class LocalMediaSortOption(val titleArabic: String) {
    DATE_DESC("الأحدث أولاً"),
    NAME_ASC("الاسم (أ-ي)"),
    FOLDER("المجلد")
}

enum class LinkSourceType(val titleArabic: String, val hostsSummary: String) {
    YOUTUBE("يوتيوب", "youtube.com, youtu.be"),
    INSTAGRAM("إنستغرام", "instagram.com"),
    TIKTOK("تيك توك", "tiktok.com, vm.tiktok.com"),
    FACEBOOK("فيسبوك", "facebook.com, fb.watch, fb.com"),
    WHATSAPP("واتساب", "whatsapp.com, wa.me"),
    TELEGRAM("تيليجرام", "t.me, telegram.me"),
    SPOTIFY("سبوتيفاي", "spotify.com, open.spotify.com"),
    APPLE_MUSIC("أبل ميوزك", "music.apple.com"),
    SOUNDCLOUD("ساوند كلاود", "soundcloud.com"),
    GOOGLE_NEWS("أخبار جوجل", "news.google.com"),
    GOOGLE_SEARCH("بحث جوجل", "google.com"),
    GOOGLE_CHROME("جوجل كروم", "chrome.google.com, روابط الويب"),
    OTHER("مواقع مخصصة / أخرى", "النطاقات المخصصة وروابط البث");

    val key: String
        get() = name

    val subtitleArabic: String
        get() = hostsSummary

    companion object {
        val twelvePlatforms: List<LinkSourceType>
            get() = listOf(
                YOUTUBE,
                INSTAGRAM,
                TIKTOK,
                FACEBOOK,
                WHATSAPP,
                TELEGRAM,
                SPOTIFY,
                APPLE_MUSIC,
                SOUNDCLOUD,
                GOOGLE_NEWS,
                GOOGLE_SEARCH,
                GOOGLE_CHROME
            )
    }
}

data class LinkSourceConfig(
    val enabled: Boolean = true,
    val quality: PreferredQuality = PreferredQuality.HIGH,
    val autoPlay: Boolean = true,
    val allowDownload: Boolean = true,
    val targetCategory: SongCategory = SongCategory.IMPORTED,
    val mediaType: LocalMediaType = LocalMediaType.ALL
)

fun defaultLinkSourceConfigs(): Map<LinkSourceType, LinkSourceConfig> = mapOf(
    LinkSourceType.YOUTUBE to LinkSourceConfig(enabled = true, quality = PreferredQuality.HIGH, autoPlay = true, allowDownload = true, targetCategory = SongCategory.LATEST, mediaType = LocalMediaType.VIDEO),
    LinkSourceType.INSTAGRAM to LinkSourceConfig(enabled = true, quality = PreferredQuality.HIGH, autoPlay = true, allowDownload = true, targetCategory = SongCategory.REELS, mediaType = LocalMediaType.VIDEO),
    LinkSourceType.TIKTOK to LinkSourceConfig(enabled = true, quality = PreferredQuality.HIGH, autoPlay = true, allowDownload = true, targetCategory = SongCategory.REELS, mediaType = LocalMediaType.VIDEO),
    LinkSourceType.FACEBOOK to LinkSourceConfig(enabled = true, quality = PreferredQuality.MEDIUM, autoPlay = true, allowDownload = true, targetCategory = SongCategory.MUSIC_VIDEO, mediaType = LocalMediaType.VIDEO),
    LinkSourceType.WHATSAPP to LinkSourceConfig(enabled = true, quality = PreferredQuality.MEDIUM, autoPlay = true, allowDownload = true, targetCategory = SongCategory.IMPORTED, mediaType = LocalMediaType.ALL),
    LinkSourceType.TELEGRAM to LinkSourceConfig(enabled = true, quality = PreferredQuality.HIGH, autoPlay = true, allowDownload = true, targetCategory = SongCategory.IMPORTED, mediaType = LocalMediaType.ALL),
    LinkSourceType.SPOTIFY to LinkSourceConfig(enabled = true, quality = PreferredQuality.HIGH, autoPlay = true, allowDownload = false, targetCategory = SongCategory.FEATURED, mediaType = LocalMediaType.AUDIO),
    LinkSourceType.APPLE_MUSIC to LinkSourceConfig(enabled = true, quality = PreferredQuality.HIGH, autoPlay = true, allowDownload = false, targetCategory = SongCategory.FEATURED, mediaType = LocalMediaType.AUDIO),
    LinkSourceType.SOUNDCLOUD to LinkSourceConfig(enabled = true, quality = PreferredQuality.MEDIUM, autoPlay = true, allowDownload = true, targetCategory = SongCategory.LATEST, mediaType = LocalMediaType.AUDIO),
    LinkSourceType.GOOGLE_NEWS to LinkSourceConfig(enabled = true, quality = PreferredQuality.AUTO, autoPlay = false, allowDownload = false, targetCategory = SongCategory.IMPORTED, mediaType = LocalMediaType.ALL),
    LinkSourceType.GOOGLE_SEARCH to LinkSourceConfig(enabled = true, quality = PreferredQuality.AUTO, autoPlay = true, allowDownload = true, targetCategory = SongCategory.IMPORTED, mediaType = LocalMediaType.ALL),
    LinkSourceType.GOOGLE_CHROME to LinkSourceConfig(enabled = true, quality = PreferredQuality.AUTO, autoPlay = false, allowDownload = true, targetCategory = SongCategory.IMPORTED, mediaType = LocalMediaType.ALL),
    LinkSourceType.OTHER to LinkSourceConfig(enabled = true, quality = PreferredQuality.MEDIUM, autoPlay = true, allowDownload = true, targetCategory = SongCategory.IMPORTED, mediaType = LocalMediaType.ALL)
)

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.YOUTUBE_DARK,
    val accentColor: AppAccentColor = AppAccentColor.RED,
    val showLocalDeviceFiles: Boolean = true,
    val showOfficialChannelOnly: Boolean = false,
    val autoPlayNext: Boolean = true,
    val autoRepeat: Boolean = false,
    val rememberPlaybackPosition: Boolean = true,
    val lyricsSyncEnabled: Boolean = true,
    val preferredQuality: PreferredQuality = PreferredQuality.HIGH,
    val defaultDownloadType: String = "ASK", // ASK, AUDIO_ONLY, FULL_VIDEO
    val downloadQuality: PreferredQuality = PreferredQuality.HIGH,
    val downloadStorageLocation: DownloadStorageLocation = DownloadStorageLocation.APP_AND_DEVICE,
    val downloadOverWifiOnly: Boolean = false,
    val autoRetryDownload: Boolean = true,
    val defaultVideoMode: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val videoAspectMode: VideoAspectMode = VideoAspectMode.FIT,
    val silentWebFallbackEnabled: Boolean = true,
    val newReleasesNotification: Boolean = true,
    val vibrateOnNotification: Boolean = true,
    val syncReleasesAutomatically: Boolean = true,
    val sleepTimerMinutes: Int = 0, // 0 means Off
    val isGridView: Boolean = false,
    val hideCategoriesBar: Boolean = false,
    val hiddenCategories: Set<SongCategory> = emptySet(),
    val linkSources: Map<LinkSourceType, LinkSourceConfig> = defaultLinkSourceConfigs(),
    val customLinkDomains: List<String> = emptyList(),
    val grantedLocalFolderUris: List<String> = emptyList()
) {
    val pushNotificationsEnabled: Boolean
        get() = newReleasesNotification

    val customSourceConfig: LinkSourceConfig
        get() = configFor(LinkSourceType.OTHER)

    val allLinkSourcesEnabled: Boolean
        get() = LinkSourceType.entries.all { linkSources[it]?.enabled != false }

    fun configFor(type: LinkSourceType): LinkSourceConfig =
        linkSources[type] ?: LinkSourceConfig()

    fun isCategoryVisible(category: SongCategory): Boolean =
        category == SongCategory.ALL || category !in hiddenCategories
}
