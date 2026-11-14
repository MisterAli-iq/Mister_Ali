package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.remote.GroundedSearchUiState
import com.example.data.remote.GroundedWebSource
import com.example.ui.components.GroundedSearchCard
import com.example.ui.theme.RahmaRiadTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchGroundingTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun groundedSearchCardSuccessDisplaysAnswerAndHandlesPlayableSourceAndDismiss() {
        var playedSourceUri: String? = null
        var dismissed = false

        val successState = GroundedSearchUiState.Success(
            query = "أحدث أغاني رحمة رياض",
            text = "أطلقت رحمة رياض أحدث أعمالها عبر قناتها الرسمية @RahmaRiad.",
            webQueries = listOf("رحمة رياض أحدث أغاني", "@RahmaRiad YouTube"),
            sources = listOf(
                GroundedWebSource(
                    title = "قناة رحمة رياض الرسمية",
                    uri = "https://www.youtube.com/watch?v=official123",
                    domain = "youtube.com"
                )
            )
        )

        composeRule.setContent {
            RahmaRiadTheme {
                GroundedSearchCard(
                    state = successState,
                    onRetry = {},
                    onDismiss = { dismissed = true },
                    onPlayMediaSource = { playedSourceUri = it }
                )
            }
        }

        composeRule.onNodeWithTag("grounded_search_card").assertIsDisplayed()
        composeRule.onNodeWithTag("grounded_search_answer_text")
            .assertIsDisplayed()
            .assertTextContains("أطلقت رحمة رياض", substring = true)

        composeRule.onAllNodesWithTag("grounded_source_chip").onFirst().performClick()
        assertEquals("https://www.youtube.com/watch?v=official123", playedSourceUri)

        composeRule.onNodeWithTag("dismiss_grounded_search_button").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun groundedSearchCardErrorDisplaysMessageAndInvokesRetry() {
        var retriedQuery: String? = null

        val errorState = GroundedSearchUiState.Error(
            query = "حفلات رحمة رياض 2026",
            message = "تعذر الاتصال بخدمة البحث المباشر."
        )

        composeRule.setContent {
            RahmaRiadTheme {
                GroundedSearchCard(
                    state = errorState,
                    onRetry = { retriedQuery = it },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("grounded_search_error_box").assertIsDisplayed()
        composeRule.onNodeWithTag("retry_grounded_search_button")
            .assertIsDisplayed()
            .performClick()

        assertEquals("حفلات رحمة رياض 2026", retriedQuery)
    }
}
