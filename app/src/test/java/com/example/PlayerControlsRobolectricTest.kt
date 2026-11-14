package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.common.PlaybackException
import com.example.model.PreferredQuality
import com.example.model.Song
import com.example.model.SongCategory
import com.example.ui.components.NativeMediaPlayerView
import com.example.ui.components.YouTubePlayer
import com.example.ui.theme.RahmaRiadTheme
import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerControlsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleSong() = Song(
        id = "test_song_1",
        title = "Rahma Riad - Waed Menni",
        arabicTitle = "رحمة رياض - وعد مني",
        category = SongCategory.LATEST,
        durationSeconds = 215,
        durationFormatted = "3:35",
        youtubeVideoId = "testVid123",
        sourcePlatform = "رحمة رياض الرسمية",
        channelHandle = "@RahmaRiad"
    )

    @Test
    fun inlinePlayer_displaysPlayPauseQualityAndFullscreenAndTriggersCallbacks() {
        var playPauseClicked = false
        var fullscreenToggled = false
        var selectedQuality: PreferredQuality? = null

        composeRule.setContent {
            RahmaRiadTheme {
                YouTubePlayer(
                    song = sampleSong(),
                    exoPlayer = null,
                    isPlaying = false,
                    isResolvingStream = false,
                    isBuffering = false,
                    isFullscreen = false,
                    activeQuality = PreferredQuality.HIGH,
                    isVideoMode = true,
                    currentPositionSeconds = 12,
                    durationSeconds = 215,
                    playbackSpeed = 1.0f,
                    isLooping = false,
                    isLyricsExpanded = false,
                    isFavorite = false,
                    onClose = {},
                    onToggleFullscreen = { fullscreenToggled = true },
                    onChangeQuality = { selectedQuality = it },
                    onTogglePlayPause = { playPauseClicked = true },
                    onSeekTo = {},
                    onSkipForward = {},
                    onSkipBackward = {},
                    onPlayNext = {},
                    onPlayPrevious = {},
                    onToggleVideoAudio = {},
                    onCycleSpeed = {},
                    onToggleLoop = {},
                    onToggleLyrics = {},
                    onFavoriteToggle = {},
                    onDownloadClick = {},
                    onSelectUpNextSong = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_center_play_pause").assertIsDisplayed().performClick()
        assertTrue(playPauseClicked)

        composeRule.onNodeWithTag("player_fullscreen_button").assertIsDisplayed().performClick()
        assertTrue(fullscreenToggled)

        composeRule.onNodeWithTag("player_overlay_quality_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("اختر جودة التشغيل").assertIsDisplayed()
        composeRule.onNodeWithText(PreferredQuality.DATA_SAVER.titleArabic)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(PreferredQuality.DATA_SAVER, selectedQuality)
    }

    @Test
    fun fullscreenPlayer_displaysFullscreenOverlayControlsAndExitsOnClick() {
        var exitedFullscreen = false
        var toggledPlayPause = false

        composeRule.setContent {
            RahmaRiadTheme {
                YouTubePlayer(
                    song = sampleSong(),
                    exoPlayer = null,
                    isPlaying = true,
                    isResolvingStream = false,
                    isBuffering = false,
                    isFullscreen = true,
                    activeQuality = PreferredQuality.MEDIUM,
                    isVideoMode = true,
                    currentPositionSeconds = 30,
                    durationSeconds = 215,
                    playbackSpeed = 1.0f,
                    isLooping = false,
                    isLyricsExpanded = false,
                    isFavorite = false,
                    onClose = {},
                    onToggleFullscreen = { exitedFullscreen = true },
                    onTogglePlayPause = { toggledPlayPause = true },
                    onSeekTo = {},
                    onSkipForward = {},
                    onSkipBackward = {},
                    onPlayNext = {},
                    onPlayPrevious = {},
                    onToggleVideoAudio = {},
                    onCycleSpeed = {},
                    onToggleLoop = {},
                    onToggleLyrics = {},
                    onFavoriteToggle = {},
                    onDownloadClick = {},
                    onSelectUpNextSong = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_fullscreen_container").assertIsDisplayed()
        composeRule.onNodeWithTag("player_overlay_quality_button").assertIsDisplayed()
        composeRule.onNodeWithTag("player_center_play_pause").assertIsDisplayed().performClick()
        assertTrue(toggledPlayPause)

        composeRule.onNodeWithTag("player_fullscreen_button").assertIsDisplayed().performClick()
        assertTrue(exitedFullscreen)
    }

    @Test
    fun playerErrorView_displaysSimpleFriendlyMessageAndRetryAction() {
        var retried = false
        var loweredQuality = false

        composeRule.setContent {
            RahmaRiadTheme {
                NativeMediaPlayerView(
                    player = null,
                    isResolvingStream = false,
                    isBuffering = false,
                    errorMessage = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة",
                    onRetry = { retried = true },
                    onLowerQuality = { loweredQuality = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_error_message_text")
            .assertIsDisplayed()
            .assertTextContains("إعادة المحاولة", substring = true)

        composeRule.onNodeWithTag("player_error_retry_button").assertIsDisplayed().performClick()
        assertTrue(retried)

        composeRule.onNodeWithTag("player_error_lower_quality_button").assertIsDisplayed().performClick()
        assertTrue(loweredQuality)
    }

    @Test
    fun mapPlaybackErrorToArabic_hidesTechnicalCodes() {
        val networkError = PlaybackException("network fail", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        val httpError = PlaybackException("http 403", null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        val decoderError = PlaybackException("decoder", null, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)

        listOf(networkError, httpError, decoderError).forEach { err ->
            val msg = MainViewModel.mapPlaybackErrorToArabic(err)
            assertTrue("Friendly message should not be blank", msg.isNotBlank())
            assertFalse("Error code must stay hidden from user: $msg", msg.contains("ERROR_CODE_"))
            assertFalse("Exception class names must stay hidden from user: $msg", msg.contains("Exception"))
        }
    }

    @Test
    fun safelyDestroyWebView_guardsAgainstDuplicateDestruction() {
        val destroyedFlag = java.util.concurrent.atomic.AtomicBoolean(false)
        com.example.ui.components.safelyDestroyWebView(null, destroyedFlag)
        assertFalse(destroyedFlag.get())

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val webView = android.webkit.WebView(context)
        com.example.ui.components.safelyDestroyWebView(webView, destroyedFlag)
        assertTrue(destroyedFlag.get())

        // Second call must safely no-op without throwing
        com.example.ui.components.safelyDestroyWebView(webView, destroyedFlag)
        assertTrue(destroyedFlag.get())
    }

    @Test
    fun inlinePlayer_displaysRealPublisherNameInsteadOfEmbeddedPlayerLabel() {
        val publisherSong = sampleSong().copy(
            id = "rotana_song_1",
            sourcePlatform = "Rotana",
            channelHandle = "@Rotana",
            externalUrl = "https://www.youtube.com/watch?v=testVid123"
        )

        composeRule.setContent {
            RahmaRiadTheme {
                YouTubePlayer(
                    song = publisherSong,
                    exoPlayer = null,
                    isPlaying = false,
                    isResolvingStream = false,
                    isBuffering = false,
                    isFullscreen = false,
                    activeQuality = PreferredQuality.HIGH,
                    isVideoMode = true,
                    currentPositionSeconds = 0,
                    durationSeconds = 215,
                    playbackSpeed = 1.0f,
                    isLooping = false,
                    isLyricsExpanded = false,
                    isFavorite = false,
                    onClose = {},
                    onToggleFullscreen = {},
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSkipForward = {},
                    onSkipBackward = {},
                    onPlayNext = {},
                    onPlayPrevious = {},
                    onToggleVideoAudio = {},
                    onCycleSpeed = {},
                    onToggleLoop = {},
                    onToggleLyrics = {},
                    onFavoriteToggle = {},
                    onDownloadClick = {},
                    onSelectUpNextSong = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_real_publisher_name")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("الناشر: Rotana", substring = true)
    }
}
