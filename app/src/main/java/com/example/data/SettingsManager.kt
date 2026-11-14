package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppAccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.DownloadStorageLocation
import com.example.model.LinkSourceConfig
import com.example.model.LinkSourceType
import com.example.model.LocalMediaType
import com.example.model.PreferredQuality
import com.example.model.SongCategory
import com.example.model.VideoAspectMode
import com.example.model.defaultLinkSourceConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rahma_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_SHOW_LOCAL = "show_local_files"
        private const val KEY_OFFICIAL_ONLY = "official_channel_only"
        private const val KEY_AUTO_PLAY = "auto_play_next"
        private const val KEY_AUTO_REPEAT = "auto_repeat"
        private const val KEY_REMEMBER_POS = "remember_position"
        private const val KEY_LYRICS_SYNC = "lyrics_sync_enabled"
        private const val KEY_QUALITY = "preferred_quality"
        private const val KEY_DL_TYPE = "default_download_type"
        private const val KEY_DL_QUALITY = "download_quality"
        private const val KEY_DL_STORAGE = "download_storage_location"
        private const val KEY_DL_WIFI_ONLY = "download_wifi_only"
        private const val KEY_DL_AUTO_RETRY = "download_auto_retry"
        private const val KEY_DEFAULT_VIDEO_MODE = "default_video_mode"
        private const val KEY_DEFAULT_SPEED = "default_playback_speed"
        private const val KEY_VIDEO_ASPECT = "video_aspect_mode"
        private const val KEY_SILENT_WEB_FALLBACK = "silent_web_fallback"
        private const val KEY_NOTIFICATIONS = "push_notifications"
        private const val KEY_VIBRATE_NOTIFICATION = "vibrate_notification"
        private const val KEY_AUTO_SYNC = "auto_sync_releases"
        private const val KEY_SLEEP_TIMER = "sleep_timer_minutes"
        private const val KEY_IS_GRID_VIEW = "is_grid_view"
        private const val KEY_HIDE_CATEGORIES_BAR = "hide_categories_bar"
        private const val KEY_HIDDEN_CATEGORIES = "hidden_categories"
        private const val KEY_CUSTOM_LINK_DOMAINS = "custom_link_domains"
        private const val KEY_GRANTED_LOCAL_FOLDERS = "granted_local_folders"
        private const val KEY_CHANNEL_ID = "official_channel_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_WELCOME_SEEN = "welcome_seen"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME_MODE, AppThemeMode.YOUTUBE_DARK.name)
        val themeMode = runCatching { AppThemeMode.valueOf(themeStr ?: "") }.getOrDefault(AppThemeMode.YOUTUBE_DARK)

        val accentStr = prefs.getString(KEY_ACCENT_COLOR, AppAccentColor.RED.name)
        val accentColor = runCatching { AppAccentColor.valueOf(accentStr ?: "") }.getOrDefault(AppAccentColor.RED)

        val qualityStr = prefs.getString(KEY_QUALITY, PreferredQuality.HIGH.name)
        val quality = runCatching { PreferredQuality.valueOf(qualityStr ?: "") }.getOrDefault(PreferredQuality.HIGH)

        val dlQualityStr = prefs.getString(KEY_DL_QUALITY, PreferredQuality.HIGH.name)
        val dlQuality = runCatching { PreferredQuality.valueOf(dlQualityStr ?: "") }.getOrDefault(PreferredQuality.HIGH)

        val dlStorageStr = prefs.getString(KEY_DL_STORAGE, DownloadStorageLocation.APP_AND_DEVICE.name)
        val dlStorage = runCatching { DownloadStorageLocation.valueOf(dlStorageStr ?: "") }.getOrDefault(DownloadStorageLocation.APP_AND_DEVICE)

        val aspectStr = prefs.getString(KEY_VIDEO_ASPECT, VideoAspectMode.FIT.name)
        val videoAspect = runCatching { VideoAspectMode.valueOf(aspectStr ?: "") }.getOrDefault(VideoAspectMode.FIT)

        val hiddenCatsRaw = prefs.getString(KEY_HIDDEN_CATEGORIES, "") ?: ""
        val hiddenCategories = hiddenCatsRaw.split("|")
            .mapNotNull { token ->
                val clean = token.trim()
                if (clean.isEmpty()) null else runCatching { SongCategory.valueOf(clean) }.getOrNull()
            }
            .filter { it != SongCategory.ALL }
            .toSet()

        return AppSettings(
            themeMode = themeMode,
            accentColor = accentColor,
            showLocalDeviceFiles = prefs.getBoolean(KEY_SHOW_LOCAL, true),
            showOfficialChannelOnly = prefs.getBoolean(KEY_OFFICIAL_ONLY, false),
            autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY, true),
            autoRepeat = prefs.getBoolean(KEY_AUTO_REPEAT, false),
            rememberPlaybackPosition = prefs.getBoolean(KEY_REMEMBER_POS, true),
            lyricsSyncEnabled = prefs.getBoolean(KEY_LYRICS_SYNC, true),
            preferredQuality = quality,
            defaultDownloadType = prefs.getString(KEY_DL_TYPE, "ASK") ?: "ASK",
            downloadQuality = dlQuality,
            downloadStorageLocation = dlStorage,
            downloadOverWifiOnly = prefs.getBoolean(KEY_DL_WIFI_ONLY, false),
            autoRetryDownload = prefs.getBoolean(KEY_DL_AUTO_RETRY, true),
            defaultVideoMode = prefs.getBoolean(KEY_DEFAULT_VIDEO_MODE, true),
            defaultPlaybackSpeed = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f),
            videoAspectMode = videoAspect,
            silentWebFallbackEnabled = prefs.getBoolean(KEY_SILENT_WEB_FALLBACK, true),
            newReleasesNotification = prefs.getBoolean(KEY_NOTIFICATIONS, true),
            vibrateOnNotification = prefs.getBoolean(KEY_VIBRATE_NOTIFICATION, true),
            syncReleasesAutomatically = prefs.getBoolean(KEY_AUTO_SYNC, true),
            sleepTimerMinutes = prefs.getInt(KEY_SLEEP_TIMER, 0),
            isGridView = prefs.getBoolean(KEY_IS_GRID_VIEW, false),
            hideCategoriesBar = prefs.getBoolean(KEY_HIDE_CATEGORIES_BAR, false),
            hiddenCategories = hiddenCategories,
            linkSources = loadLinkSourceConfigs(),
            customLinkDomains = loadStringList(KEY_CUSTOM_LINK_DOMAINS),
            grantedLocalFolderUris = loadStringList(KEY_GRANTED_LOCAL_FOLDERS)
        )
    }

    private fun loadLinkSourceConfigs(): Map<LinkSourceType, LinkSourceConfig> {
        val defaults = defaultLinkSourceConfigs()
        return LinkSourceType.entries.associateWith { type ->
            val def = defaults[type] ?: LinkSourceConfig()
            val qName = prefs.getString("src_quality_${type.name}", def.quality.name)
            val parsedQ = runCatching { PreferredQuality.valueOf(qName ?: "") }.getOrDefault(def.quality)
            val catName = prefs.getString("src_category_${type.name}", def.targetCategory.name)
            val parsedCat = if (catName.isNullOrBlank()) def.targetCategory else SongCategory.fromStoredName(catName)
            val mediaName = prefs.getString("src_mediatype_${type.name}", def.mediaType.name)
            val parsedMedia = runCatching { LocalMediaType.valueOf(mediaName ?: "") }.getOrDefault(def.mediaType)
            LinkSourceConfig(
                enabled = prefs.getBoolean("src_enabled_${type.name}", def.enabled),
                quality = parsedQ,
                autoPlay = prefs.getBoolean("src_autoplay_${type.name}", def.autoPlay),
                allowDownload = prefs.getBoolean("src_download_${type.name}", def.allowDownload),
                targetCategory = parsedCat,
                mediaType = parsedMedia
            )
        }
    }

    private fun loadStringList(key: String): List<String> {
        val raw = prefs.getString(key, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun saveStringList(key: String, list: List<String>) {
        prefs.edit().putString(key, list.distinct().joinToString("|")).apply()
    }

    fun applyAllSettings(newSettings: AppSettings) {
        val editor = prefs.edit()
            .putString(KEY_THEME_MODE, newSettings.themeMode.name)
            .putString(KEY_ACCENT_COLOR, newSettings.accentColor.name)
            .putBoolean(KEY_SHOW_LOCAL, newSettings.showLocalDeviceFiles)
            .putBoolean(KEY_OFFICIAL_ONLY, newSettings.showOfficialChannelOnly)
            .putBoolean(KEY_AUTO_PLAY, newSettings.autoPlayNext)
            .putBoolean(KEY_AUTO_REPEAT, newSettings.autoRepeat)
            .putBoolean(KEY_REMEMBER_POS, newSettings.rememberPlaybackPosition)
            .putBoolean(KEY_LYRICS_SYNC, newSettings.lyricsSyncEnabled)
            .putString(KEY_QUALITY, newSettings.preferredQuality.name)
            .putString(KEY_DL_TYPE, newSettings.defaultDownloadType)
            .putString(KEY_DL_QUALITY, newSettings.downloadQuality.name)
            .putString(KEY_DL_STORAGE, newSettings.downloadStorageLocation.name)
            .putBoolean(KEY_DL_WIFI_ONLY, newSettings.downloadOverWifiOnly)
            .putBoolean(KEY_DL_AUTO_RETRY, newSettings.autoRetryDownload)
            .putBoolean(KEY_DEFAULT_VIDEO_MODE, newSettings.defaultVideoMode)
            .putFloat(KEY_DEFAULT_SPEED, newSettings.defaultPlaybackSpeed)
            .putString(KEY_VIDEO_ASPECT, newSettings.videoAspectMode.name)
            .putBoolean(KEY_SILENT_WEB_FALLBACK, newSettings.silentWebFallbackEnabled)
            .putBoolean(KEY_NOTIFICATIONS, newSettings.newReleasesNotification)
            .putBoolean(KEY_VIBRATE_NOTIFICATION, newSettings.vibrateOnNotification)
            .putBoolean(KEY_AUTO_SYNC, newSettings.syncReleasesAutomatically)
            .putInt(KEY_SLEEP_TIMER, newSettings.sleepTimerMinutes)
            .putBoolean(KEY_IS_GRID_VIEW, newSettings.isGridView)
            .putBoolean(KEY_HIDE_CATEGORIES_BAR, newSettings.hideCategoriesBar)
            .putString(
                KEY_HIDDEN_CATEGORIES,
                newSettings.hiddenCategories.filter { it != SongCategory.ALL }.joinToString("|") { it.name }
            )
            .putString(KEY_CUSTOM_LINK_DOMAINS, newSettings.customLinkDomains.distinct().joinToString("|"))
            .putString(KEY_GRANTED_LOCAL_FOLDERS, newSettings.grantedLocalFolderUris.distinct().joinToString("|"))

        LinkSourceType.entries.forEach { type ->
            val cfg = newSettings.configFor(type)
            editor
                .putBoolean("src_enabled_${type.name}", cfg.enabled)
                .putString("src_quality_${type.name}", cfg.quality.name)
                .putBoolean("src_autoplay_${type.name}", cfg.autoPlay)
                .putBoolean("src_download_${type.name}", cfg.allowDownload)
                .putString("src_category_${type.name}", cfg.targetCategory.name)
                .putString("src_mediatype_${type.name}", cfg.mediaType.name)
        }
        editor.apply()
        _settings.value = newSettings
    }

    fun resetToDefaults() {
        val preservedFolders = _settings.value.grantedLocalFolderUris
        val fresh = AppSettings(grantedLocalFolderUris = preservedFolders)
        applyAllSettings(fresh)
    }

    fun updateGridView(isGrid: Boolean) {
        prefs.edit().putBoolean(KEY_IS_GRID_VIEW, isGrid).apply()
        _settings.value = _settings.value.copy(isGridView = isGrid)
    }

    fun updateHideCategoriesBar(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_CATEGORIES_BAR, hide).apply()
        _settings.value = _settings.value.copy(hideCategoriesBar = hide)
    }

    fun setCategoryHidden(category: SongCategory, hidden: Boolean) {
        if (category == SongCategory.ALL) return
        val current = _settings.value.hiddenCategories
        val updated = if (hidden) current + category else current - category
        prefs.edit().putString(KEY_HIDDEN_CATEGORIES, updated.joinToString("|") { it.name }).apply()
        _settings.value = _settings.value.copy(hiddenCategories = updated)
    }

    fun setLinkSourceEnabled(type: LinkSourceType, enabled: Boolean) {
        val currentMap = _settings.value.linkSources.toMutableMap()
        val updated = _settings.value.configFor(type).copy(enabled = enabled)
        currentMap[type] = updated
        persistLinkSourceConfig(type, updated)
        _settings.value = _settings.value.copy(linkSources = currentMap)
    }

    fun setAllLinkSourcesEnabled(enabled: Boolean) {
        val updatedMap = LinkSourceType.entries.associateWith { type ->
            val cfg = _settings.value.configFor(type).copy(enabled = enabled)
            persistLinkSourceConfig(type, cfg)
            cfg
        }
        _settings.value = _settings.value.copy(linkSources = updatedMap)
    }

    fun updateLinkSourceConfig(type: LinkSourceType, config: LinkSourceConfig) {
        val currentMap = _settings.value.linkSources.toMutableMap()
        currentMap[type] = config
        persistLinkSourceConfig(type, config)
        _settings.value = _settings.value.copy(linkSources = currentMap)
    }

    fun updateCustomSourceConfig(config: LinkSourceConfig) {
        updateLinkSourceConfig(LinkSourceType.OTHER, config)
    }

    private fun persistLinkSourceConfig(type: LinkSourceType, cfg: LinkSourceConfig) {
        prefs.edit()
            .putBoolean("src_enabled_${type.name}", cfg.enabled)
            .putString("src_quality_${type.name}", cfg.quality.name)
            .putBoolean("src_autoplay_${type.name}", cfg.autoPlay)
            .putBoolean("src_download_${type.name}", cfg.allowDownload)
            .putString("src_category_${type.name}", cfg.targetCategory.name)
            .putString("src_mediatype_${type.name}", cfg.mediaType.name)
            .apply()
    }

    fun addCustomLinkDomain(domain: String): String? {
        val cleaned = cleanHost(domain) ?: return null
        val updated = (_settings.value.customLinkDomains + cleaned).distinct()
        saveStringList(KEY_CUSTOM_LINK_DOMAINS, updated)
        _settings.value = _settings.value.copy(customLinkDomains = updated)
        return cleaned
    }

    fun removeCustomLinkDomain(domain: String) {
        val updated = _settings.value.customLinkDomains.filterNot { it.equals(domain, ignoreCase = true) }
        saveStringList(KEY_CUSTOM_LINK_DOMAINS, updated)
        _settings.value = _settings.value.copy(customLinkDomains = updated)
    }

    fun addGrantedLocalFolderUri(uriString: String) {
        val clean = uriString.trim()
        if (clean.isBlank()) return
        val updated = (_settings.value.grantedLocalFolderUris + clean).distinct()
        saveStringList(KEY_GRANTED_LOCAL_FOLDERS, updated)
        _settings.value = _settings.value.copy(grantedLocalFolderUris = updated)
    }

    fun removeGrantedLocalFolderUri(uriString: String) {
        val updated = _settings.value.grantedLocalFolderUris.filterNot { it == uriString }
        saveStringList(KEY_GRANTED_LOCAL_FOLDERS, updated)
        _settings.value = _settings.value.copy(grantedLocalFolderUris = updated)
    }

    private fun cleanHost(raw: String): String? {
        val trimmed = raw.trim().lowercase()
        if (trimmed.isBlank()) return null
        val withoutScheme = trimmed
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore(":")
            .removePrefix("www.")
            .removePrefix("m.")
        return if (withoutScheme.contains(".") && withoutScheme.length >= 4 && !withoutScheme.contains(" ")) {
            withoutScheme
        } else {
            null
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateAccentColor(color: AppAccentColor) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color.name).apply()
        _settings.value = _settings.value.copy(accentColor = color)
    }

    fun updateShowLocalFiles(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LOCAL, show).apply()
        _settings.value = _settings.value.copy(showLocalDeviceFiles = show)
    }

    fun updateShowOfficialOnly(officialOnly: Boolean) {
        prefs.edit().putBoolean(KEY_OFFICIAL_ONLY, officialOnly).apply()
        _settings.value = _settings.value.copy(showOfficialChannelOnly = officialOnly)
    }

    fun updateAutoPlayNext(autoPlay: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, autoPlay).apply()
        _settings.value = _settings.value.copy(autoPlayNext = autoPlay)
    }

    fun updateAutoPlay(autoPlay: Boolean) = updateAutoPlayNext(autoPlay)

    fun updateAutoRepeat(autoRepeat: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REPEAT, autoRepeat).apply()
        _settings.value = _settings.value.copy(autoRepeat = autoRepeat)
    }

    fun updateRememberPosition(remember: Boolean) = setRememberPlaybackPosition(remember)

    fun updateLyricsSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LYRICS_SYNC, enabled).apply()
        _settings.value = _settings.value.copy(lyricsSyncEnabled = enabled)
    }

    fun updateQuality(quality: PreferredQuality) {
        prefs.edit().putString(KEY_QUALITY, quality.name).apply()
        _settings.value = _settings.value.copy(preferredQuality = quality)
    }

    fun updateWifiOnly(wifiOnly: Boolean) = setDownloadOverWifiOnly(wifiOnly)

    fun updateNotifications(enabled: Boolean) = updatePushNotifications(enabled)

    fun updatePushNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _settings.value = _settings.value.copy(newReleasesNotification = enabled)
    }

    fun updateVibrate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_NOTIFICATION, enabled).apply()
        _settings.value = _settings.value.copy(vibrateOnNotification = enabled)
    }

    fun updateAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        _settings.value = _settings.value.copy(syncReleasesAutomatically = enabled)
    }

    fun updateSleepTimer(minutes: Int) {
        prefs.edit().putInt(KEY_SLEEP_TIMER, minutes).apply()
        _settings.value = _settings.value.copy(sleepTimerMinutes = minutes)
    }

    fun setDefaultDownloadType(type: String) {
        prefs.edit().putString(KEY_DL_TYPE, type).apply()
        _settings.value = _settings.value.copy(defaultDownloadType = type)
    }

    fun setDownloadQuality(quality: PreferredQuality) {
        prefs.edit().putString(KEY_DL_QUALITY, quality.name).apply()
        _settings.value = _settings.value.copy(downloadQuality = quality)
    }

    fun setDownloadStorageLocation(location: DownloadStorageLocation) {
        prefs.edit().putString(KEY_DL_STORAGE, location.name).apply()
        _settings.value = _settings.value.copy(downloadStorageLocation = location)
    }

    fun setDownloadOverWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_DL_WIFI_ONLY, wifiOnly).apply()
        _settings.value = _settings.value.copy(downloadOverWifiOnly = wifiOnly)
    }

    fun setAutoRetryDownload(retry: Boolean) {
        prefs.edit().putBoolean(KEY_DL_AUTO_RETRY, retry).apply()
        _settings.value = _settings.value.copy(autoRetryDownload = retry)
    }

    fun setDefaultVideoMode(videoMode: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_VIDEO_MODE, videoMode).apply()
        _settings.value = _settings.value.copy(defaultVideoMode = videoMode)
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_DEFAULT_SPEED, speed).apply()
        _settings.value = _settings.value.copy(defaultPlaybackSpeed = speed)
    }

    fun setVideoAspectMode(mode: VideoAspectMode) {
        prefs.edit().putString(KEY_VIDEO_ASPECT, mode.name).apply()
        _settings.value = _settings.value.copy(videoAspectMode = mode)
    }

    fun setSilentWebFallbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SILENT_WEB_FALLBACK, enabled).apply()
        _settings.value = _settings.value.copy(silentWebFallbackEnabled = enabled)
    }

    fun setRememberPlaybackPosition(remember: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_POS, remember).apply()
        _settings.value = _settings.value.copy(rememberPlaybackPosition = remember)
    }

    fun savePlaybackPosition(songId: String, positionSeconds: Int) {
        if (_settings.value.rememberPlaybackPosition && positionSeconds > 0) {
            prefs.edit().putInt("pos_$songId", positionSeconds).apply()
        }
    }

    fun getPlaybackPosition(songId: String): Int {
        return if (_settings.value.rememberPlaybackPosition) {
            prefs.getInt("pos_$songId", 0)
        } else 0
    }

    fun saveUserSession(name: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putBoolean(KEY_USER_LOGGED_IN, true)
            .apply()
    }

    fun getSavedUserSession(): Pair<String, String>? {
        val isLogged = prefs.getBoolean(KEY_USER_LOGGED_IN, false)
        return if (isLogged) {
            val name = prefs.getString(KEY_USER_NAME, "محب رحمة رياض") ?: "محب رحمة رياض"
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            Pair(name, email)
        } else null
    }

    fun clearUserSession() {
        prefs.edit().remove(KEY_USER_NAME).remove(KEY_USER_EMAIL).putBoolean(KEY_USER_LOGGED_IN, false).apply()
    }

    fun isWelcomeSeen(): Boolean = prefs.getBoolean(KEY_WELCOME_SEEN, false)

    fun setWelcomeSeen() {
        prefs.edit().putBoolean(KEY_WELCOME_SEEN, true).apply()
    }

    fun saveOfficialChannelId(channelId: String) {
        prefs.edit().putString(KEY_CHANNEL_ID, channelId).apply()
    }

    fun getOfficialChannelId(): String? = prefs.getString(KEY_CHANNEL_ID, null)

    fun setLastSyncTime(timeMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timeMillis).apply()
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
}
