package com.example
 
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import com.example.model.AppAccentColor
import com.example.model.AppThemeMode
import com.example.model.PreferredQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
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
}
