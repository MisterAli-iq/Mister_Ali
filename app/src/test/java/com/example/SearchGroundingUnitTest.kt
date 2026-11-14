package com.example

import com.example.data.remote.SearchGroundingService
import com.google.firebase.ai.type.GroundingChunk
import com.google.firebase.ai.type.GroundingMetadata
import com.google.firebase.ai.type.WebGroundingChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchGroundingUnitTest {

    @Test
    fun buildSearchPromptFormatsBlankArtistAndGeneralQueriesProperly() {
        val blankPrompt = SearchGroundingService.buildSearchPrompt("   ")
        assertTrue(blankPrompt.contains("@RahmaRiad"))
        assertTrue(blankPrompt.contains("Google"))

        val artistPrompt = SearchGroundingService.buildSearchPrompt("حفلات رحمة رياض الجديدة")
        assertTrue(artistPrompt.contains("حفلات رحمة رياض الجديدة"))
        assertTrue(artistPrompt.contains("Google"))

        val generalPrompt = SearchGroundingService.buildSearchPrompt("الكوكب")
        assertTrue(generalPrompt.contains("@RahmaRiad"))
        assertTrue(generalPrompt.contains("الكوكب"))
    }

    @Test
    fun extractGroundingDataDeduplicatesQueriesAndWebSourcesWithFallbacks() {
        val (emptyQueries, emptySources) = SearchGroundingService.extractGroundingData(null)
        assertTrue(emptyQueries.isEmpty())
        assertTrue(emptySources.isEmpty())

        val metadata = GroundingMetadata(
            webSearchQueries = listOf("  رحمة رياض  ", "رحمة رياض", "", "Rahma Riad concerts"),
            searchEntryPoint = null,
            retrievalQueries = emptyList(),
            groundingAttribution = emptyList(),
            groundingChunks = listOf(
                GroundingChunk(
                    web = WebGroundingChunk(
                        uri = "https://www.youtube.com/watch?v=abc123",
                        title = "رحمة رياض - الكوكب",
                        domain = "youtube.com"
                    )
                ),
                GroundingChunk(
                    web = WebGroundingChunk(
                        uri = "https://www.youtube.com/watch?v=abc123",
                        title = "Duplicate URI",
                        domain = "youtube.com"
                    )
                ),
                GroundingChunk(
                    web = WebGroundingChunk(
                        uri = "https://billboardarabia.com/rahma",
                        title = "   ",
                        domain = "billboardarabia.com"
                    )
                ),
                GroundingChunk(
                    web = WebGroundingChunk(
                        uri = "   ",
                        title = "Blank URI ignored",
                        domain = "ignored.example"
                    )
                )
            ),
            groundingSupports = emptyList()
        )

        val (queries, sources) = SearchGroundingService.extractGroundingData(metadata)
        assertEquals(listOf("رحمة رياض", "Rahma Riad concerts"), queries)
        assertEquals(2, sources.size)
        assertEquals("رحمة رياض - الكوكب", sources[0].title)
        assertEquals("billboardarabia.com", sources[1].title)
    }
}
