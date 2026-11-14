package com.example
 
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import com.example.model.AppAccentColor
import com.example.model.AppThemeMode
import com.example.model.DownloadStorageLocation
import com.example.model.PreferredQuality
import com.example.model.VideoAspectMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SettingsManagerTest {

    private lateinit var settingsManager: SettingsManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        settingsManager = SettingsManager(context)
    }

    @Test
    fun testDefaultSettings() {
        val settings = settingsManager.settings.value
        assertEquals(AppThemeMode.YOUTUBE_DARK, settings.themeMode)
        assertEquals(AppAccentColor.RED, settings.accentColor)
        assertTrue(settings.showLocalDeviceFiles)
        assertTrue(settings.autoPlayNext)
        assertTrue(settings.rememberPlaybackPosition)
        assertEquals(PreferredQuality.HIGH, settings.preferredQuality)
        assertEquals("ASK", settings.defaultDownloadType)
        assertEquals(PreferredQuality.HIGH, settings.downloadQuality)
        assertEquals(DownloadStorageLocation.APP_AND_DEVICE, settings.downloadStorageLocation)
        assertTrue(settings.autoRetryDownload)
        assertTrue(settings.defaultVideoMode)
        assertEquals(1.0f, settings.defaultPlaybackSpeed, 0.001f)
        assertEquals(VideoAspectMode.FIT, settings.videoAspectMode)
        assertTrue(settings.silentWebFallbackEnabled)
    }

    @Test
    fun testUpdateThemeModeAndAccent() {
        settingsManager.updateThemeMode(AppThemeMode.AMOLED_BLACK)
        assertEquals(AppThemeMode.AMOLED_BLACK, settingsManager.settings.value.themeMode)

        settingsManager.updateAccentColor(AppAccentColor.GOLD)
        assertEquals(AppAccentColor.GOLD, settingsManager.settings.value.accentColor)
    }

    @Test
    fun testUpdateLocalFilesToggle() {
        settingsManager.updateShowLocalFiles(false)
        assertFalse(settingsManager.settings.value.showLocalDeviceFiles)

        settingsManager.updateShowLocalFiles(true)
        assertTrue(settingsManager.settings.value.showLocalDeviceFiles)
    }

    @Test
    fun testUpdateSleepTimerAndQuality() {
        settingsManager.updateSleepTimer(30)
        assertEquals(30, settingsManager.settings.value.sleepTimerMinutes)

        settingsManager.updateQuality(PreferredQuality.MEDIUM)
        assertEquals(PreferredQuality.MEDIUM, settingsManager.settings.value.preferredQuality)
    }

    @Test
    fun testUpdateDownloadSettingsPersistence() {
        settingsManager.setDefaultDownloadType("AUDIO_ONLY")
        settingsManager.setDownloadQuality(PreferredQuality.DATA_SAVER)
        settingsManager.setDownloadStorageLocation(DownloadStorageLocation.APP_ONLY)
        settingsManager.setDownloadOverWifiOnly(true)
        settingsManager.setAutoRetryDownload(false)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val reloaded = SettingsManager(context).settings.value
        assertEquals("AUDIO_ONLY", reloaded.defaultDownloadType)
        assertEquals(PreferredQuality.DATA_SAVER, reloaded.downloadQuality)
        assertEquals(DownloadStorageLocation.APP_ONLY, reloaded.downloadStorageLocation)
        assertTrue(reloaded.downloadOverWifiOnly)
        assertFalse(reloaded.autoRetryDownload)
    }

    @Test
    fun testUpdateVideoSettingsAndPlaybackPositionMemory() {
        settingsManager.setDefaultVideoMode(false)
        settingsManager.setDefaultPlaybackSpeed(1.5f)
        settingsManager.setVideoAspectMode(VideoAspectMode.CROP)
        settingsManager.setSilentWebFallbackEnabled(false)
        settingsManager.setRememberPlaybackPosition(true)
        settingsManager.savePlaybackPosition("song_rahma_1", 95)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val reloadedManager = SettingsManager(context)
        val reloaded = reloadedManager.settings.value
        assertFalse(reloaded.defaultVideoMode)
        assertEquals(1.5f, reloaded.defaultPlaybackSpeed, 0.001f)
        assertEquals(VideoAspectMode.CROP, reloaded.videoAspectMode)
        assertFalse(reloaded.silentWebFallbackEnabled)
        assertEquals(95, reloadedManager.getPlaybackPosition("song_rahma_1"))

        // When disabled, getPlaybackPosition must return 0
        reloadedManager.setRememberPlaybackPosition(false)
        assertEquals(0, reloadedManager.getPlaybackPosition("song_rahma_1"))
    }

    @Test
    fun testLinkSourcesPersistenceAndAllToggle() {
        settingsManager.setLinkSourceEnabled(com.example.model.LinkSourceType.TIKTOK, false)
        settingsManager.updateLinkSourceConfig(
            com.example.model.LinkSourceType.INSTAGRAM,
            com.example.model.LinkSourceConfig(
                enabled = true,
                quality = PreferredQuality.DATA_SAVER,
                autoPlay = false,
                allowDownload = false
            )
        )
        assertEquals("vimeo.com", settingsManager.addCustomLinkDomain("https://www.vimeo.com/12345"))
        settingsManager.addGrantedLocalFolderUri("content://com.android.externalstorage.documents/tree/primary%3AMusic")

        val context = ApplicationProvider.getApplicationContext<Context>()
        val reloaded = SettingsManager(context)
        val s = reloaded.settings.value

        assertFalse(s.allLinkSourcesEnabled)
        assertFalse(s.configFor(com.example.model.LinkSourceType.TIKTOK).enabled)
        val ig = s.configFor(com.example.model.LinkSourceType.INSTAGRAM)
        assertTrue(ig.enabled)
        assertEquals(PreferredQuality.DATA_SAVER, ig.quality)
        assertFalse(ig.autoPlay)
        assertFalse(ig.allowDownload)
        assertTrue(s.customLinkDomains.contains("vimeo.com"))
        assertTrue(s.grantedLocalFolderUris.contains("content://com.android.externalstorage.documents/tree/primary%3AMusic"))

        reloaded.setAllLinkSourcesEnabled(true)
        assertTrue(reloaded.settings.value.allLinkSourcesEnabled)
        assertTrue(reloaded.settings.value.configFor(com.example.model.LinkSourceType.TIKTOK).enabled)

        reloaded.removeCustomLinkDomain("vimeo.com")
        assertFalse(reloaded.settings.value.customLinkDomains.contains("vimeo.com"))
    }

    @Test
    fun testApplyAllSettingsGridViewCategoriesVisibilityAndReset() {
        settingsManager.updateGridView(true)
        settingsManager.updateHideCategoriesBar(true)
        settingsManager.setCategoryHidden(com.example.model.SongCategory.REELS, true)
        settingsManager.setCategoryHidden(com.example.model.SongCategory.MUSIC_VIDEO, true)
        // ALL category cannot be hidden
        settingsManager.setCategoryHidden(com.example.model.SongCategory.ALL, true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val reloaded = SettingsManager(context)
        val s = reloaded.settings.value
        assertTrue(s.isGridView)
        assertTrue(s.hideCategoriesBar)
        assertTrue(s.hiddenCategories.contains(com.example.model.SongCategory.REELS))
        assertTrue(s.hiddenCategories.contains(com.example.model.SongCategory.MUSIC_VIDEO))
        assertFalse(s.hiddenCategories.contains(com.example.model.SongCategory.ALL))

        // Batch applyAllSettings updates multiple fields atomically
        val batch = s.copy(
            preferredQuality = PreferredQuality.STANDARD_480,
            isGridView = false,
            hideCategoriesBar = false,
            hiddenCategories = setOf(com.example.model.SongCategory.CONCERTS)
        )
        reloaded.applyAllSettings(batch)
        val afterBatch = SettingsManager(context).settings.value
        assertEquals(PreferredQuality.STANDARD_480, afterBatch.preferredQuality)
        assertFalse(afterBatch.isGridView)
        assertFalse(afterBatch.hideCategoriesBar)
        assertEquals(setOf(com.example.model.SongCategory.CONCERTS), afterBatch.hiddenCategories)

        // resetToDefaults restores defaults while preserving grantedLocalFolderUris
        reloaded.addGrantedLocalFolderUri("content://folder/preserved")
        reloaded.resetToDefaults()
        val afterReset = SettingsManager(context).settings.value
        assertEquals(PreferredQuality.HIGH, afterReset.preferredQuality)
        assertTrue(afterReset.hiddenCategories.isEmpty())
        assertTrue(afterReset.grantedLocalFolderUris.contains("content://folder/preserved"))
    }
}
