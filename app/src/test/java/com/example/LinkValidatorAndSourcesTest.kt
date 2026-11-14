package com.example

import com.example.data.local.LocalFileEntity
import com.example.data.local.LocalMediaScanner
import com.example.data.remote.LinkCheckStatus
import com.example.data.remote.LinkValidator
import com.example.model.AppSettings
import com.example.model.LinkSourceConfig
import com.example.model.LinkSourceType
import com.example.model.LocalMediaSortOption
import com.example.model.LocalMediaType
import com.example.model.PreferredQuality
import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkValidatorAndSourcesTest {

    @Test
    fun `sanitizeAndAutoCorrect repairs missing scheme typos whitespace and tracking params`() {
        val messy = "  شاهد الفيديو htps://m.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=ig_web_copy_link&fbclid=123   "
        val fixed = LinkValidator.sanitizeAndAutoCorrect(messy)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", fixed)

        val noScheme = "fb.watch/abcd1234/"
        assertEquals("https://fb.watch/abcd1234/", LinkValidator.sanitizeAndAutoCorrect(noScheme))
    }

    @Test
    fun `detectSource identifies built-in platforms and custom domains`() {
        val yt = LinkValidator.detectSource("https://youtu.be/abc123")
        assertEquals(LinkSourceType.YOUTUBE, yt.sourceType)

        val ig = LinkValidator.detectSource("https://www.instagram.com/reel/xyz/")
        assertEquals(LinkSourceType.INSTAGRAM, ig.sourceType)

        val tt = LinkValidator.detectSource("https://vm.tiktok.com/ZM12345/")
        assertEquals(LinkSourceType.TIKTOK, tt.sourceType)

        val fb = LinkValidator.detectSource("https://fb.watch/demo/")
        assertEquals(LinkSourceType.FACEBOOK, fb.sourceType)

        assertEquals(LinkSourceType.SPOTIFY, LinkValidator.detectSource("https://open.spotify.com/track/123").sourceType)
        assertEquals(LinkSourceType.TELEGRAM, LinkValidator.detectSource("https://t.me/RahmaRiad/10").sourceType)
        assertEquals(LinkSourceType.WHATSAPP, LinkValidator.detectSource("https://whatsapp.com/channel/0029").sourceType)
        assertEquals(LinkSourceType.GOOGLE_NEWS, LinkValidator.detectSource("https://news.google.com/search?q=Rahma").sourceType)
        assertEquals(LinkSourceType.SOUNDCLOUD, LinkValidator.detectSource("https://soundcloud.com/rahma-riad/waed").sourceType)

        val custom = LinkValidator.detectSource(
            url = "https://player.vimeo.com/video/12345",
            customDomains = listOf("vimeo.com")
        )
        assertTrue(custom.isCustomDomainMatch)
        assertEquals("vimeo.com", custom.matchedDomain)
    }

    @Test
    fun `inspectLocally flags disabled source and malformed links with clear Arabic reasons`() {
        val disabledSettings = AppSettings(
            linkSources = com.example.model.defaultLinkSourceConfigs().toMutableMap().apply {
                put(LinkSourceType.TIKTOK, LinkSourceConfig(enabled = false))
            }
        )

        val disabledResult = LinkValidator.inspectLocally("vm.tiktok.com/ZTest", disabledSettings)
        assertEquals(LinkCheckStatus.SOURCE_DISABLED, disabledResult.status)
        assertFalse(disabledResult.isSourceEnabled)
        assertTrue(disabledResult.wasAutoCorrected)
        assertEquals("https://vm.tiktok.com/ZTest", disabledResult.correctedUrl)
        assertTrue(disabledResult.exactReason!!.contains("معطّل"))

        val invalidResult = LinkValidator.inspectLocally("https://notadomain", AppSettings())
        assertEquals(LinkCheckStatus.INVALID, invalidResult.status)
        assertFalse(invalidResult.exactReason.isNullOrBlank())

        val fixedValid = LinkValidator.inspectLocally("  youtube.com/shorts/rahma123  ", AppSettings())
        assertEquals(LinkCheckStatus.FIXED_AND_VALID, fixedValid.status)
        assertEquals("https://youtube.com/shorts/rahma123", fixedValid.correctedUrl)
    }

    @Test
    fun `filterAndSortLocalMedia handles videos audio images search and folder sorting`() {
        val sample = listOf(
            LocalFileEntity(
                id = "1",
                uriString = "content://local/1",
                displayName = "Rahma_Clip.mp4",
                sizeFormatted = "15 MB",
                mimeType = "video/mp4",
                durationFormatted = "3:20",
                mediaType = "VIDEO",
                folderName = "Movies",
                dateModified = 2000L,
                addedAt = 2000L
            ),
            LocalFileEntity(
                id = "2",
                uriString = "content://local/2",
                displayName = "Concert_Photo.jpg",
                sizeFormatted = "2 MB",
                mimeType = "image/jpeg",
                durationFormatted = "صورة",
                mediaType = "IMAGE",
                folderName = "Camera",
                dateModified = 3000L,
                addedAt = 3000L
            ),
            LocalFileEntity(
                id = "3",
                uriString = "content://local/3",
                displayName = "Asaad_Gamar.mp3",
                sizeFormatted = "5 MB",
                mimeType = "audio/mpeg",
                durationFormatted = "4:10",
                mediaType = "AUDIO",
                folderName = "Download",
                dateModified = 1000L,
                addedAt = 1000L
            )
        )

        val onlyImages = LocalMediaScanner.filterAndSortLocalMedia(
            items = sample,
            query = "",
            typeFilter = LocalMediaType.IMAGE,
            sortOption = LocalMediaSortOption.DATE_DESC
        )
        assertEquals(1, onlyImages.size)
        assertEquals("2", onlyImages.first().id)

        val searchMusic = LocalMediaScanner.filterAndSortLocalMedia(
            items = sample,
            query = "asaad",
            typeFilter = LocalMediaType.ALL,
            sortOption = LocalMediaSortOption.DATE_DESC
        )
        assertEquals(1, searchMusic.size)
        assertEquals("3", searchMusic.first().id)

        val sortedByFolder = LocalMediaScanner.filterAndSortLocalMedia(
            items = sample,
            query = "",
            typeFilter = LocalMediaType.ALL,
            sortOption = LocalMediaSortOption.FOLDER
        )
        assertEquals(listOf("Camera", "Download", "Movies"), sortedByFolder.map { it.folderName })
    }

    @Test
    fun `nextLowerQuality steps down cleanly for weak network recovery`() {
        assertEquals(PreferredQuality.MEDIUM, MainViewModel.nextLowerQuality(PreferredQuality.HIGH))
        assertEquals(PreferredQuality.MEDIUM, MainViewModel.nextLowerQuality(PreferredQuality.AUTO))
        assertEquals(PreferredQuality.STANDARD_480, MainViewModel.nextLowerQuality(PreferredQuality.MEDIUM))
        assertEquals(PreferredQuality.LOW_360, MainViewModel.nextLowerQuality(PreferredQuality.STANDARD_480))
        assertEquals(PreferredQuality.DATA_SAVER, MainViewModel.nextLowerQuality(PreferredQuality.LOW_360))
        assertEquals(PreferredQuality.DATA_SAVER, MainViewModel.nextLowerQuality(PreferredQuality.DATA_SAVER))
    }

    @Test
    fun `inspectLocally distinguishes supported vs unsupported URLs and reports minor syntax errors`() {
        val settings = AppSettings()

        // Minor syntax errors on supported YouTube link
        val typoYt = LinkValidator.inspectLocally("htpps:\\\\youtube..com/watch?v=rahma1&utm_source=ig", settings)
        assertEquals(LinkCheckStatus.FIXED_AND_VALID, typoYt.status)
        assertTrue(typoYt.isSupported)
        assertTrue(typoYt.hasMinorSyntaxErrors)
        assertTrue(typoYt.syntaxIssues.isNotEmpty())
        assertEquals("https://youtube.com/watch?v=rahma1", typoYt.correctedUrl)

        // Direct MP4 media URL is supported even on generic CDN
        val directMp4 = LinkValidator.inspectLocally("https://cdn.example.com/media/clip.mp4", settings)
        assertEquals(LinkCheckStatus.VALID, directMp4.status)
        assertTrue(directMp4.isSupported)
        assertFalse(directMp4.hasMinorSyntaxErrors)

        // Unsupported protocol
        val ftpUrl = LinkValidator.inspectLocally("ftp://files.example.com/clip.mp4", settings)
        assertEquals(LinkCheckStatus.UNSUPPORTED, ftpUrl.status)
        assertFalse(ftpUrl.isSupported)
        assertFalse(ftpUrl.canOfferCustomDomain)

        // Generic non-media domain not in custom domains is flagged UNSUPPORTED with custom domain option
        val unknownSite = LinkValidator.inspectLocally("https://wikipedia.org/wiki/Rahma_Riad", settings)
        assertEquals(LinkCheckStatus.UNSUPPORTED, unknownSite.status)
        assertFalse(unknownSite.isSupported)
        assertTrue(unknownSite.canOfferCustomDomain)

        // Adding the domain to customLinkDomains makes it supported
        val withCustom = settings.copy(customLinkDomains = listOf("wikipedia.org"))
        val nowSupported = LinkValidator.inspectLocally("https://wikipedia.org/wiki/Rahma_Riad", withCustom)
        assertEquals(LinkCheckStatus.VALID, nowSupported.status)
        assertTrue(nowSupported.isSupported)
    }
}
