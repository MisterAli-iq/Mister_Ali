package com.example

import com.example.data.RahmaCatalog
import com.example.data.remote.MediaStreamResolver
import com.example.data.remote.OfficialChannelFeedService
import com.example.model.PreferredQuality
import com.example.model.Song
import com.example.model.SongCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class OfficialInstagramFeedRobolectricTest {

    @Before
    fun setUp() {
        MediaStreamResolver.clearStreamCache()
    }

    @Test
    fun parseOfficialInstagramProfileJson_extractsReelsAndPreCachesStream() {
        val mockJson = """
            {
              "data": {
                "user": {
                  "edge_owner_to_timeline_media": {
                    "edges": [
                      {
                        "node": {
                          "shortcode": "C9RahmaReel1",
                          "is_video": true,
                          "product_type": "clips",
                          "video_url": "https://cdn.example.com/rahma_reel_1.mp4",
                          "display_url": "https://cdn.example.com/rahma_thumb_1.jpg",
                          "video_view_count": 125000,
                          "taken_at_timestamp": 1720000000,
                          "edge_media_to_caption": {
                            "edges": [
                              { "node": { "text": "كواليس حفل بغداد ✨\n#رحمة_رياض" } }
                            ]
                          }
                        }
                      },
                      {
                        "node": {
                          "shortcode": "C9PhotoOnly",
                          "is_video": false,
                          "display_url": "https://cdn.example.com/photo_only.jpg"
                        }
                      }
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val items = OfficialChannelFeedService.parseInstagramProfileJson(mockJson, "rahmariadh")
        assertEquals(1, items.size)
        val reel = items.first()
        assertEquals("ig_C9RahmaReel1", reel.id)
        assertEquals("https://www.instagram.com/reel/C9RahmaReel1/", reel.sourceUrl)
        assertEquals("@rahmariadh", reel.channelHandle)
        assertTrue(reel.isShort)
        assertTrue(reel.isOfficialChannel)

        val cached = MediaStreamResolver.getCachedStream(
            sourceUrl = "https://www.instagram.com/reel/C9RahmaReel1/",
            preferredQuality = PreferredQuality.HIGH
        )
        assertEquals("https://cdn.example.com/rahma_reel_1.mp4", cached?.streamUrl)

        val igSong = Song(
            id = reel.id,
            title = reel.title,
            arabicTitle = reel.title,
            category = SongCategory.REELS,
            externalUrl = reel.sourceUrl,
            channelHandle = reel.channelHandle,
            sourcePlatform = reel.channelName,
            isShort = true
        )
        assertTrue(RahmaCatalog.isOfficialRahmaChannel(igSong))
        assertEquals(RahmaCatalog.OFFICIAL_INSTAGRAM_AR_NAME, RahmaCatalog.displayChannelName(igSong))
    }

    @Test
    fun parseOfficialInstagramProfileHtml_extractsReelShortcodes() {
        val html = """
            <html><body>
              <a href="/rahmariadh/reel/D1TestReel99/">Reel</a>
              <a href="/p/D1TestPost88/">Post</a>
            </body></html>
        """.trimIndent()

        val items = OfficialChannelFeedService.parseInstagramProfileHtml(html, "rahmariadh")
        assertEquals(2, items.size)
        assertEquals("https://www.instagram.com/reel/D1TestReel99/", items[0].sourceUrl)
        assertEquals("@rahmariadh", items[0].channelHandle)
        assertTrue(items[0].isOfficialChannel)
    }
}
