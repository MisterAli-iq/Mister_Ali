package com.example

import com.example.data.RahmaCatalog
import com.example.model.SongCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun catalog_containsFamousSongs() {
        val songs = RahmaCatalog.songs
        assertTrue("Catalog should contain at least 10 songs", songs.size >= 10)

        val kawkab = RahmaCatalog.getSongById("alkawkab")
        assertNotNull("Al Kawkab should exist", kawkab)
        assertEquals("الكوكب", kawkab?.arabicTitle)

        val waed = RahmaCatalog.getSongById("waed_menni")
        assertNotNull("Waed Menni should exist", waed)
    }

    @Test
    fun catalog_filterByCategory_worksCorrectly() {
        val clips = RahmaCatalog.filterByCategory(SongCategory.CLIPS)
        assertTrue("Clips should not be empty", clips.isNotEmpty())
        assertTrue("All returned should be CLIPS", clips.all { it.category == SongCategory.CLIPS })

        val all = RahmaCatalog.filterByCategory(SongCategory.ALL)
        assertEquals(RahmaCatalog.songs.size, all.size)
    }

    @Test
    fun catalog_searchSongs_findsMatches() {
        val results = RahmaCatalog.searchSongs("الكوكب")
        assertTrue("Should find results for 'الكوكب'", results.isNotEmpty())
        assertEquals("alkawkab", results.first().id)

        val lyricsSearch = RahmaCatalog.searchSongs("المريخ")
        assertTrue("Should find Al Kawkab by lyric search", lyricsSearch.any { it.id == "alkawkab" })
    }
}
