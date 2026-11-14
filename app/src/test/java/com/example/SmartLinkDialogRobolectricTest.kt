package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.remote.LinkValidator
import com.example.model.AppSettings
import com.example.ui.components.SmartLinkDialog
import com.example.ui.theme.RahmaRiadTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartLinkDialogRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pastedUrlWithMinorSyntaxErrorShowsInstantFeedbackAndFixButtonRepairsInput() {
        composeRule.setContent {
            RahmaRiadTheme {
                var settings by remember { mutableStateOf(AppSettings()) }
                var linkState by remember { mutableStateOf(LinkValidator.inspectLocally("", settings)) }

                SmartLinkDialog(
                    linkCheckState = linkState,
                    onInspectLink = { raw -> linkState = LinkValidator.inspectLocally(raw, settings) },
                    onCheckLink = { raw -> linkState = LinkValidator.inspectLocally(raw, settings) },
                    onEnableSourceNow = {},
                    onAddCustomDomain = { domain ->
                        settings = settings.copy(customLinkDomains = settings.customLinkDomains + domain)
                        linkState = LinkValidator.inspectLocally(linkState.rawInput, settings)
                    },
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Enter URL with minor syntax typo (htps:// instead of https://)
        composeRule.onNodeWithTag("smart_link_input")
            .performTextReplacement("htps://youtube.com/watch?v=dQw4w9WgXcQ")

        // Instant support badge and Fix card appear immediately
        composeRule.onNodeWithTag("link_support_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("minor_syntax_fix_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("fix_link_button").performScrollTo().assertIsDisplayed()

        // Tap Fix button -> repairs input field and transitions status cleanly to valid_link_status_box
        composeRule.onNodeWithTag("fix_link_button").performClick()

        composeRule.onNodeWithTag("smart_link_input")
            .performScrollTo()
            .assertTextContains("https://youtube.com/watch?v=dQw4w9WgXcQ")
        composeRule.onNodeWithTag("minor_syntax_fix_card").assertDoesNotExist()
        composeRule.onNodeWithTag("valid_link_status_box").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("smart_link_confirm_button").assertIsEnabled()
    }

    @Test
    fun unsupportedUrlShowsInstantUnsupportedFeedbackAndDisablesConfirm() {
        composeRule.setContent {
            RahmaRiadTheme {
                val settings = AppSettings()
                var linkState by remember { mutableStateOf(LinkValidator.inspectLocally("", settings)) }

                SmartLinkDialog(
                    linkCheckState = linkState,
                    onInspectLink = { raw -> linkState = LinkValidator.inspectLocally(raw, settings) },
                    onCheckLink = { raw -> linkState = LinkValidator.inspectLocally(raw, settings) },
                    onEnableSourceNow = {},
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("smart_link_input")
            .performTextReplacement("ftp://files.example.com/audio.mp3")

        composeRule.onNodeWithTag("link_support_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("unsupported_link_box").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("smart_link_confirm_button").assertIsNotEnabled()
    }
}
