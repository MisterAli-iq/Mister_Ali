package com.example.model

enum class AppThemeMode(val titleArabic: String, val subtitleArabic: String) {
    YOUTUBE_DARK("داكن يوتيوب (Dark)", "النمط الكلاسيكي الداكن مع لمسات يوتيوب الحمراء"),
    AMOLED_BLACK("أسود أموليد (AMOLED)", "خلفية سوداء بالكامل لأعلى توفير للطاقة وإراحة العين"),
    LIGHT("فاتح كلاسيكي (Light)", "مظهر نهاري مشرق ونظيف"),
    ROYAL_GOLD("ذهبي رحمة الملكي (Gold)", "مظهر فاخر مستوحى من ألوان الذهب العراقي")
}

enum class AppAccentColor(val titleArabic: String, val hex: Long) {
    RED("أحمر يوتيوب", 0xFFFF0033),
    GOLD("ذهبي ملكي", 0xFFFFC107),
    BLUE("أزرق سماوي", 0xFF29B6F6),
    GREEN("أخضر زمردي", 0xFF4CAF50),
    PURPLE("بنفسجي طربي", 0xFFAB47BC)
}

enum class PreferredQuality(val titleArabic: String) {
    AUTO("تلقائي (حسب سرعة الاتصال)"),
    HIGH("جودة عالية جداً (FHD / 320kbps)"),
    MEDIUM("جودة قياسية (HD / 192kbps)"),
    DATA_SAVER("توفير البيانات (SD / 128kbps)")
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.YOUTUBE_DARK,
    val accentColor: AppAccentColor = AppAccentColor.RED,
    val showLocalDeviceFiles: Boolean = true,
    val autoPlayNext: Boolean = true,
    val rememberPlaybackPosition: Boolean = true,
    val preferredQuality: PreferredQuality = PreferredQuality.HIGH,
    val autoRepeat: Boolean = false,
    val lyricsSyncEnabled: Boolean = true,
    val downloadOverWifiOnly: Boolean = false,
    val defaultDownloadType: String = "ASK", // "ASK", "FULL_VIDEO", "AUDIO_ONLY"
    val newReleasesNotification: Boolean = true,
    val vibrateOnNotification: Boolean = true,
    val sleepTimerMinutes: Int = 0 // 0 = off, 15, 30, 45, 60
)
