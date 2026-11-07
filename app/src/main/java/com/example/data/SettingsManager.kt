package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppAccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.PreferredQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rahma_riad_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.YOUTUBE_DARK.name)
        val themeMode = runCatching { AppThemeMode.valueOf(themeName ?: "") }.getOrDefault(AppThemeMode.YOUTUBE_DARK)

        val accentName = prefs.getString(KEY_ACCENT_COLOR, AppAccentColor.RED.name)
        val accentColor = runCatching { AppAccentColor.valueOf(accentName ?: "") }.getOrDefault(AppAccentColor.RED)

        val qualityName = prefs.getString(KEY_QUALITY, PreferredQuality.HIGH.name)
        val quality = runCatching { PreferredQuality.valueOf(qualityName ?: "") }.getOrDefault(PreferredQuality.HIGH)

        return AppSettings(
            themeMode = themeMode,
            accentColor = accentColor,
            showLocalDeviceFiles = prefs.getBoolean(KEY_SHOW_LOCAL_FILES, true),
            autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true),
            rememberPlaybackPosition = prefs.getBoolean(KEY_REMEMBER_POSITION, true),
            preferredQuality = quality,
            autoRepeat = prefs.getBoolean(KEY_AUTO_REPEAT, false),
            lyricsSyncEnabled = prefs.getBoolean(KEY_LYRICS_SYNC, true),
            downloadOverWifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            defaultDownloadType = prefs.getString(KEY_DOWNLOAD_TYPE, "ASK") ?: "ASK",
            newReleasesNotification = prefs.getBoolean(KEY_NOTIFICATIONS, true),
            vibrateOnNotification = prefs.getBoolean(KEY_VIBRATE, true),
            sleepTimerMinutes = prefs.getInt(KEY_SLEEP_TIMER, 0)
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAccentColor(accent: AppAccentColor) {
        prefs.edit().putString(KEY_ACCENT_COLOR, accent.name).apply()
        _settings.value = _settings.value.copy(accentColor = accent)
    }

    fun setShowLocalDeviceFiles(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LOCAL_FILES, show).apply()
        _settings.value = _settings.value.copy(showLocalDeviceFiles = show)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, enabled).apply()
        _settings.value = _settings.value.copy(autoPlayNext = enabled)
    }

    fun setRememberPlaybackPosition(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_POSITION, enabled).apply()
        _settings.value = _settings.value.copy(rememberPlaybackPosition = enabled)
    }

    fun setPreferredQuality(quality: PreferredQuality) {
        prefs.edit().putString(KEY_QUALITY, quality.name).apply()
        _settings.value = _settings.value.copy(preferredQuality = quality)
    }

    fun setAutoRepeat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REPEAT, enabled).apply()
        _settings.value = _settings.value.copy(autoRepeat = enabled)
    }

    fun setLyricsSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LYRICS_SYNC, enabled).apply()
        _settings.value = _settings.value.copy(lyricsSyncEnabled = enabled)
    }

    fun setDownloadOverWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
        _settings.value = _settings.value.copy(downloadOverWifiOnly = enabled)
    }

    fun setDefaultDownloadType(type: String) {
        prefs.edit().putString(KEY_DOWNLOAD_TYPE, type).apply()
        _settings.value = _settings.value.copy(defaultDownloadType = type)
    }

    fun setNewReleasesNotification(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _settings.value = _settings.value.copy(newReleasesNotification = enabled)
    }

    fun setVibrateOnNotification(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _settings.value = _settings.value.copy(vibrateOnNotification = enabled)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SLEEP_TIMER, minutes).apply()
        _settings.value = _settings.value.copy(sleepTimerMinutes = minutes)
    }

    // Convenient aliases
    fun updateThemeMode(mode: AppThemeMode) = setThemeMode(mode)
    fun updateAccentColor(accent: AppAccentColor) = setAccentColor(accent)
    fun updateShowLocalFiles(show: Boolean) = setShowLocalDeviceFiles(show)
    fun updateAutoPlayNext(enabled: Boolean) = setAutoPlayNext(enabled)
    fun updateRememberPosition(enabled: Boolean) = setRememberPlaybackPosition(enabled)
    fun updateQuality(quality: PreferredQuality) = setPreferredQuality(quality)
    fun updateAutoRepeat(enabled: Boolean) = setAutoRepeat(enabled)
    fun updateLyricsSync(enabled: Boolean) = setLyricsSyncEnabled(enabled)
    fun updateWifiOnly(enabled: Boolean) = setDownloadOverWifiOnly(enabled)
    fun updateNotifications(enabled: Boolean) = setNewReleasesNotification(enabled)
    fun updateVibrate(enabled: Boolean) = setVibrateOnNotification(enabled)
    fun updateSleepTimer(minutes: Int) = setSleepTimerMinutes(minutes)

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ACCENT_COLOR = "key_accent_color"
        private const val KEY_SHOW_LOCAL_FILES = "key_show_local_files"
        private const val KEY_AUTO_PLAY_NEXT = "key_auto_play_next"
        private const val KEY_REMEMBER_POSITION = "key_remember_position"
        private const val KEY_QUALITY = "key_quality"
        private const val KEY_AUTO_REPEAT = "key_auto_repeat"
        private const val KEY_LYRICS_SYNC = "key_lyrics_sync"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_DOWNLOAD_TYPE = "key_download_type"
        private const val KEY_NOTIFICATIONS = "key_notifications"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_SLEEP_TIMER = "key_sleep_timer"
    }
}
