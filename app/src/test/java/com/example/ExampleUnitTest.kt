package com.example

import com.example.data.RahmaCatalog
import com.example.data.remote.MediaStreamResolver
import com.example.data.remote.StreamFormatCandidate
import com.example.model.PreferredQuality
import com.example.model.RahmaReel
import com.example.model.ReelFilter
import com.example.model.Song
import com.example.model.SongCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun sampleInputSongs(): List<Song> = listOf(
        Song(
            id = "s1",
            title = "Live Concert Baghdad",
            arabicTitle = "حفل بغداد المباشر",
            category = SongCategory.CONCERTS,
            sourcePlatform = "القناة الرسمية",
            isFeatured = true
        ),
        Song(
            id = "s2",
            title = "Instagram Reel",
            arabicTitle = "ريلز انستغرام مباشر",
            category = SongCategory.REELS,
            sourcePlatform = "انستغرام",
            isShort = true,
            isUserImported = true,
            isFeatured = false
        ),
        Song(
            id = "s3",
            title = "TikTok Clip",
            arabicTitle = "مقطع تيك توك",
            category = SongCategory.IMPORTED,
            sourcePlatform = "تيك توك",
            isShort = true,
            isUserImported = true,
            isFeatured = true
        )
    )

    @Test
    fun catalog_filterByCategory_worksCorrectly() {
        val list = sampleInputSongs()

        val all = RahmaCatalog.filterByCategory(list, SongCategory.ALL)
        assertEquals(3, all.size)

        val featured = RahmaCatalog.filterByCategory(list, SongCategory.FEATURED)
        assertEquals(2, featured.size)
        assertTrue(featured.all { it.isFeatured })

        val concerts = RahmaCatalog.filterByCategory(list, SongCategory.CONCERTS)
        assertEquals(1, concerts.size)
        assertEquals("s1", concerts.first().id)

        val reels = RahmaCatalog.filterByCategory(list, SongCategory.REELS)
        assertEquals(2, reels.size)

        val imported = RahmaCatalog.filterByCategory(list, SongCategory.IMPORTED)
        assertEquals(2, imported.size)
        assertTrue(imported.all { it.isUserImported })
    }

    @Test
    fun catalog_searchSongs_findsMatches() {
        val list = sampleInputSongs()

        val results = RahmaCatalog.searchSongs(list, "بغداد")
        assertEquals(1, results.size)
        assertEquals("s1", results.first().id)

        val platformResults = RahmaCatalog.searchSongs(list, "تيك توك")
        assertEquals(1, platformResults.size)
        assertEquals("s3", platformResults.first().id)
    }

    @Test
    fun catalog_mergeAndRankSmartSearch_prioritizesOfficialChannelAndDeduplicates() {
        val localSongs = listOf(
            Song(
                id = "local_1",
                title = "Rahma Riad - Waed Menni",
                arabicTitle = "رحمة رياض - وعد مني",
                category = SongCategory.ALL,
                youtubeVideoId = "yt_official_1",
                isFeatured = true,
                sourcePlatform = "رحمة رياض الرسمية",
                channelHandle = "@RahmaRiad"
            )
        )
        val remoteSongs = listOf(
            Song(
                id = "remote_fan",
                title = "Rahma Riad - Waed Menni Fan Cover",
                arabicTitle = "وعد مني - أداء معجبين",
                category = SongCategory.ALL,
                youtubeVideoId = "yt_fan_2",
                sourcePlatform = "Rotana",
                channelHandle = "@Rotana"
            ),
            Song(
                id = "remote_dup_official",
                title = "Rahma Riad - Waed Menni",
                arabicTitle = "رحمة رياض - وعد مني",
                category = SongCategory.ALL,
                youtubeVideoId = "yt_official_1",
                sourcePlatform = "رحمة رياض الرسمية",
                channelHandle = "@RahmaRiad"
            ),
            Song(
                id = "remote_official_2",
                title = "Rahma Riad - Al Kawkab",
                arabicTitle = "رحمة رياض - الكوكب",
                category = SongCategory.ALL,
                youtubeVideoId = "yt_official_3",
                sourcePlatform = "Rahma Riad | رحمة رياض",
                channelHandle = "@RahmaRiad"
            )
        )

        val merged = RahmaCatalog.mergeAndRankSmartSearch(localSongs, remoteSongs, "رحمة رياض")

        // Deduplicated yt_official_1 -> 3 unique songs total
        assertEquals(3, merged.size)
        // Official @RahmaRiad tracks must appear before third-party channels (Rotana)
        assertTrue(RahmaCatalog.isOfficialRahmaChannel(merged[0]))
        assertTrue(RahmaCatalog.isOfficialRahmaChannel(merged[1]))
        assertFalse(RahmaCatalog.isOfficialRahmaChannel(merged[2]))
        assertEquals("Rotana", RahmaCatalog.displayChannelName(merged[2]))
        // Preserves local featured flag on deduplicated item
        val deduped = merged.first { it.youtubeVideoId == "yt_official_1" }
        assertTrue(deduped.isFeatured)
        assertEquals("local_1", deduped.id)
    }

    @Test
    fun catalog_realChannelNamesAndOfficialDetection_accuracy() {
        assertTrue(RahmaCatalog.isOfficialRahmaChannelName("Rahma Riad | رحمة رياض", "@RahmaRiad"))
        assertTrue(RahmaCatalog.isOfficialRahmaChannelName("رحمة رياض الرسمية", ""))
        assertFalse(RahmaCatalog.isOfficialRahmaChannelName("محبي رحمة رياض", ""))
        assertFalse(RahmaCatalog.isOfficialRahmaChannelName("Rahma Riad Fans", ""))
        assertFalse(RahmaCatalog.isOfficialRahmaChannelName("Rotana", "@Rotana"))
        assertFalse(RahmaCatalog.isOfficialRahmaChannelName("MBC Iraq", "@MBCIraq"))

        val officialSong = Song(
            id = "ch_off",
            title = "Official",
            arabicTitle = "وعد مني",
            category = SongCategory.LATEST,
            sourcePlatform = "Rahma Riad | رحمة رياض",
            channelHandle = "@RahmaRiad"
        )
        assertTrue(RahmaCatalog.displayChannelName(officialSong).contains("رحمة رياض الرسمية"))

        val rotanaSong = Song(
            id = "ch_rot",
            title = "Clip",
            arabicTitle = "حفل روتانا",
            category = SongCategory.CONCERTS,
            sourcePlatform = "Rotana",
            channelHandle = "@Rotana"
        )
        assertFalse(RahmaCatalog.isOfficialRahmaChannel(rotanaSong))
        assertEquals("Rotana", RahmaCatalog.displayChannelName(rotanaSong))
    }

    @Test
    fun catalog_filterReels_filtersByPlatformAndType() {
        val reels = listOf(
            RahmaReel(
                id = "r1",
                title = "Official Short",
                caption = "",
                sourceUrl = "https://www.youtube.com/shorts/abc",
                platformLabel = "القناة الرسمية",
                isUserImported = false
            ),
            RahmaReel(
                id = "r2",
                title = "IG Reel",
                caption = "",
                sourceUrl = "https://www.instagram.com/reel/xyz/",
                platformLabel = "انستغرام",
                isUserImported = true
            ),
            RahmaReel(
                id = "r3",
                title = "TT Reel",
                caption = "",
                sourceUrl = "https://www.tiktok.com/@user/video/999",
                platformLabel = "تيك توك",
                isUserImported = true
            )
        )

        assertEquals(3, RahmaCatalog.filterReels(reels, ReelFilter.ALL).size)
        assertEquals(listOf("r1"), RahmaCatalog.filterReels(reels, ReelFilter.OFFICIAL).map { it.id })
        assertEquals(listOf("r2"), RahmaCatalog.filterReels(reels, ReelFilter.INSTAGRAM).map { it.id })
        assertEquals(listOf("r3"), RahmaCatalog.filterReels(reels, ReelFilter.TIKTOK).map { it.id })
        assertEquals(listOf("r2", "r3"), RahmaCatalog.filterReels(reels, ReelFilter.IMPORTED).map { it.id })
        assertEquals(listOf("r2"), RahmaCatalog.filterReels(reels, ReelFilter.SAVED, setOf("r2")).map { it.id })
    }

    @Test
    fun streamResolver_extractsVideoIdsDirectMediaAndSilentEmbed() {
        assertEquals(
            "dQw4w9WgXcQ",
            MediaStreamResolver.extractYouTubeId("https://youtu.be/dQw4w9WgXcQ?si=123")
        )
        assertEquals(
            "abc123XYZ",
            MediaStreamResolver.extractYouTubeId("https://www.youtube.com/shorts/abc123XYZ?feature=share")
        )
        assertEquals(
            "vid999",
            MediaStreamResolver.extractYouTubeId("https://www.youtube.com/watch?v=vid999&t=12")
        )

        val silentEmbed = MediaStreamResolver.buildSilentYouTubeEmbedUrl("vid999")
        assertTrue(silentEmbed.contains("youtube-nocookie.com/embed/vid999"))
        assertTrue(silentEmbed.contains("playsinline=1"))

        assertTrue(MediaStreamResolver.isDirectMediaUri("https://cdn.example.com/video.mp4?token=1"))
        assertTrue(MediaStreamResolver.isDirectMediaUri("https://cdn.example.com/live/stream.m3u8"))
        assertTrue(MediaStreamResolver.isDirectMediaUri("https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/123"))
        assertTrue(MediaStreamResolver.isDirectMediaUri("content://media/external/video/media/42"))
        assertFalse(MediaStreamResolver.isDirectMediaUri("https://www.instagram.com/reel/C12345/"))

        assertTrue(MediaStreamResolver.isHlsStreamUrl("https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/123"))
        assertTrue(MediaStreamResolver.isHlsStreamUrl("https://cdn.example.com/master.m3u8?token=abc"))
        assertFalse(MediaStreamResolver.isHlsStreamUrl("https://cdn.example.com/video.mp4"))
    }

    @Test
    fun streamResolver_selectsBestVideoAndAudioCandidatesAccordingToQuality() {
        val videoCandidates = listOf(
            StreamFormatCandidate("https://cdn/360.mp4", heightPx = 360, bitrateBps = 500_000, mimeType = "video/mp4"),
            StreamFormatCandidate("https://cdn/720.mp4", heightPx = 720, bitrateBps = 1_500_000, mimeType = "video/mp4"),
            StreamFormatCandidate("https://cdn/1080.mp4", heightPx = 1080, bitrateBps = 3_000_000, mimeType = "video/mp4")
        )

        val highPick = MediaStreamResolver.selectBestVideoCandidate(videoCandidates, PreferredQuality.HIGH.maxHeightPx)
        assertNotNull(highPick)
        assertEquals(1080, highPick!!.heightPx)

        val mediumPick = MediaStreamResolver.selectBestVideoCandidate(videoCandidates, PreferredQuality.MEDIUM.maxHeightPx)
        assertNotNull(mediumPick)
        assertEquals(720, mediumPick!!.heightPx)

        val dataSaverPick = MediaStreamResolver.selectBestVideoCandidate(videoCandidates, PreferredQuality.DATA_SAVER.maxHeightPx)
        assertNotNull(dataSaverPick)
        assertEquals(360, dataSaverPick!!.heightPx)

        val audioCandidates = listOf(
            StreamFormatCandidate("https://cdn/opus.webm", heightPx = 0, bitrateBps = 160_000, mimeType = "audio/webm"),
            StreamFormatCandidate("https://cdn/aac_low.m4a", heightPx = 0, bitrateBps = 128_000, mimeType = "audio/mp4"),
            StreamFormatCandidate("https://cdn/aac_high.m4a", heightPx = 0, bitrateBps = 256_000, mimeType = "audio/mp4")
        )

        val highAudio = MediaStreamResolver.selectBestAudioCandidate(audioCandidates, preferHighBitrate = true)
        assertEquals("https://cdn/aac_high.m4a", highAudio?.url)

        val lowAudio = MediaStreamResolver.selectBestAudioCandidate(audioCandidates, preferHighBitrate = false)
        assertEquals("https://cdn/aac_low.m4a", lowAudio?.url)
    }
}
