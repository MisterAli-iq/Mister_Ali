package com.example

import androidx.media3.common.PlaybackException
import com.example.data.remote.MediaStreamResolver
import com.example.data.remote.ResolvedStreamResult
import com.example.model.PreferredQuality
import com.example.ui.viewmodel.MainViewModel
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class MediaStreamResolverUnitTest {

    private var server: HttpServer? = null

    @Before
    fun setUp() {
        MediaStreamResolver.clearStreamCache()
    }

    @After
    fun tearDown() {
        server?.stop(0)
        server = null
        MediaStreamResolver.clearStreamCache()
    }

    @Test
    fun isDirectMediaUri_strictlySeparatesWebPagesFromDirectStreams() {
        // Webpage / watch / reel URLs must NEVER be treated as direct playable streams
        val pageUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/abc123XYZ",
            "https://www.youtube.com/embed/abc123XYZ",
            "https://www.youtube-nocookie.com/embed/abc123XYZ",
            "https://www.instagram.com/reel/C12345/",
            "https://www.instagram.com/p/C12345/",
            "https://www.tiktok.com/@rahma/video/7345678901234",
            "https://example.com/watch/video.mp4.html",
            "https://example.com/player.php?src=test.mp4"
        )
        for (page in pageUrls) {
            assertFalse("Expected webpage URL to be rejected: $page", MediaStreamResolver.isDirectMediaUri(page))
        }

        // Direct streams and local media URIs must be accepted
        val directUrls = listOf(
            "https://cdn.example.com/clip.mp4?token=abc",
            "https://cdn.example.com/live/master.m3u8",
            "https://cdn.example.com/audio/song.m4a",
            "https://cdn.example.com/audio/song.mp3",
            "https://rr1---sn.googlevideo.com/videoplayback?expire=12345&mime=video/mp4",
            "https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/123",
            "content://media/external/video/media/77",
            "file:///data/user/0/com.example/files/offline.mp4"
        )
        for (direct in directUrls) {
            assertTrue("Expected direct media URI to be accepted: $direct", MediaStreamResolver.isDirectMediaUri(direct))
        }
    }

    @Test
    fun streamCache_cachesOnlyVerifiedDirectUrlsForInstantReplay() {
        val pageUrl = "https://www.youtube.com/watch?v=testVid01"

        // Unverified webpage link must NOT be cached
        MediaStreamResolver.putCachedStream(
            sourceUrl = pageUrl,
            videoIdHint = "testVid01",
            preferredQuality = PreferredQuality.HIGH,
            result = ResolvedStreamResult(streamUrl = pageUrl)
        )
        assertNull(MediaStreamResolver.getCachedStream(pageUrl, "testVid01", PreferredQuality.HIGH))

        // Verified direct .mp4 URL IS cached and returned immediately
        val directResult = ResolvedStreamResult(
            streamUrl = "https://cdn.example.com/stream_720.mp4",
            pageUrl = pageUrl,
            isDirectVerified = true
        )
        MediaStreamResolver.putCachedStream(
            sourceUrl = pageUrl,
            videoIdHint = "testVid01",
            preferredQuality = PreferredQuality.HIGH,
            result = directResult
        )

        val startNs = System.nanoTime()
        val cachedHit = MediaStreamResolver.getCachedStream(pageUrl, "testVid01", PreferredQuality.HIGH)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L

        assertNotNull(cachedHit)
        assertEquals("https://cdn.example.com/stream_720.mp4", cachedHit?.streamUrl)
        assertTrue(cachedHit?.isDirectVerified == true)
        assertTrue("Cache lookup must be instant (< 3000ms), was ${elapsedMs}ms", elapsedMs < 3000L)

        MediaStreamResolver.invalidateCachedStream(pageUrl, "testVid01")
        assertNull(MediaStreamResolver.getCachedStream(pageUrl, "testVid01", PreferredQuality.HIGH))
    }

    @Test
    fun resolvePlayableStream_extractsAndVerifiesRealDirectStreamFromWebpageUnder3Seconds() = runBlocking {
        val localServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server = localServer
        localServer.executor = Executors.newCachedThreadPool()
        val port = localServer.address.port
        val baseUrl = "http://127.0.0.1:$port"

        // Webpage carrying <video src="/media/rahma_clip.mp4">
        localServer.createContext("/watch-page") { exchange ->
            val html = """
                <html>
                  <head>
                    <meta property="og:title" content="رحمة رياض - بث تجريبي" />
                  </head>
                  <body>
                    <video src="$baseUrl/media/rahma_clip.mp4"></video>
                  </body>
                </html>
            """.trimIndent().toByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, html.size.toLong())
            exchange.responseBody.use { it.write(html) }
        }

        // Valid MP4 endpoint supporting HEAD & GET
        localServer.createContext("/media/rahma_clip.mp4") { exchange ->
            val bytes = ByteArray(64) { 1 }
            exchange.responseHeaders.add("Content-Type", "video/mp4")
            if (exchange.requestMethod.equals("HEAD", ignoreCase = true)) {
                exchange.sendResponseHeaders(200, -1)
                exchange.responseBody.close()
            } else {
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }

        // Broken 403 MP4 link & fake HTML disguised as .mp4
        localServer.createContext("/media/forbidden.mp4") { exchange ->
            exchange.sendResponseHeaders(403, -1)
            exchange.responseBody.close()
        }
        localServer.createContext("/media/fake_html.mp4") { exchange ->
            val body = "<html>Not a video</html>".toByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }

        localServer.start()

        // Probe rejection checks
        assertFalse(MediaStreamResolver.verifyDirectPlayableStream("$baseUrl/media/forbidden.mp4"))
        assertFalse(MediaStreamResolver.verifyDirectPlayableStream("$baseUrl/media/fake_html.mp4"))
        assertTrue(MediaStreamResolver.verifyDirectPlayableStream("$baseUrl/media/rahma_clip.mp4"))

        // End-to-end extraction from page link -> verified direct .mp4 under 3 seconds
        val startMs = System.currentTimeMillis()
        val resolved = MediaStreamResolver.resolvePlayableStream(
            sourceUrl = "$baseUrl/watch-page",
            preferredQuality = PreferredQuality.HIGH,
            verifyNetworkStream = true
        )
        val firstElapsed = System.currentTimeMillis() - startMs

        assertEquals("$baseUrl/media/rahma_clip.mp4", resolved.streamUrl)
        assertTrue(resolved.isDirectVerified)
        assertEquals("رحمة رياض - بث تجريبي", resolved.title)
        assertTrue("Expected fast extraction < 3000ms, was ${firstElapsed}ms", firstElapsed < 3000L)

        // Cached replay check after stopping server -> still resolves instantly from memory
        localServer.stop(0)
        server = null
        val replayStartMs = System.currentTimeMillis()
        val cachedReplay = MediaStreamResolver.resolvePlayableStream(
            sourceUrl = "$baseUrl/watch-page",
            preferredQuality = PreferredQuality.HIGH,
            verifyNetworkStream = true
        )
        val replayElapsed = System.currentTimeMillis() - replayStartMs
        assertEquals("$baseUrl/media/rahma_clip.mp4", cachedReplay.streamUrl)
        assertTrue("Cached replay must be < 500ms, was ${replayElapsed}ms", replayElapsed < 500L)
    }

    @Test
    fun timeoutAndQualityStepDown_behaveCorrectly() {
        assertEquals(10_000L, MediaStreamResolver.MAX_RESOLUTION_TIMEOUT_MS)

        assertEquals(PreferredQuality.MEDIUM, MainViewModel.nextLowerQuality(PreferredQuality.AUTO))
        assertEquals(PreferredQuality.MEDIUM, MainViewModel.nextLowerQuality(PreferredQuality.HIGH))
        assertEquals(PreferredQuality.STANDARD_480, MainViewModel.nextLowerQuality(PreferredQuality.MEDIUM))
        assertEquals(PreferredQuality.LOW_360, MainViewModel.nextLowerQuality(PreferredQuality.STANDARD_480))
        assertEquals(PreferredQuality.DATA_SAVER, MainViewModel.nextLowerQuality(PreferredQuality.LOW_360))
        assertEquals(PreferredQuality.DATA_SAVER, MainViewModel.nextLowerQuality(PreferredQuality.DATA_SAVER))
    }

    @Test
    fun requireVideoTrack_skipsAudioOnlyCacheAndServesAudioWhenAllowed() = runBlocking {
        val audioCacheUrl = "https://cdn.example.com/audio/only_track.m4a"
        MediaStreamResolver.putCachedStream(
            sourceUrl = audioCacheUrl,
            preferredQuality = PreferredQuality.HIGH,
            result = ResolvedStreamResult(
                streamUrl = audioCacheUrl,
                pageUrl = audioCacheUrl,
                isDirectVerified = true
            )
        )

        // When requireVideoTrack is false, cached .m4a stream is returned immediately
        val allowedAudio = MediaStreamResolver.resolvePlayableStream(
            sourceUrl = audioCacheUrl,
            preferredQuality = PreferredQuality.HIGH,
            verifyNetworkStream = false,
            requireVideoTrack = false
        )
        assertEquals(audioCacheUrl, allowedAudio.streamUrl)

        // When requireVideoTrack is true, the audio-only cached entry is bypassed rather than returning audio-only cache hit
        val cachedEntryCheck = MediaStreamResolver.getCachedStream(audioCacheUrl, preferredQuality = PreferredQuality.HIGH)
        assertNotNull(cachedEntryCheck)
        assertTrue(cachedEntryCheck!!.streamUrl!!.endsWith(".m4a"))
    }
}
