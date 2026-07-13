package io.github.mrxgamer999.openinstremio.ui.updates

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdateScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun updateAvailable_rendersVersionsNotesAndDownload() {
        composeTestRule.setContent {
            OpenInStremioTheme {
                UpdateScreen(
                    uiState =
                        UpdateUiState.UpdateAvailable(
                            currentVersion = "1.0.0",
                            latestVersion = "v1.1.0",
                            notes = listOf("Faster IMDb matching for TV episodes"),
                            url = "https://example.com",
                        ),
                    onBack = {},
                    onRetry = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Update available").assertIsDisplayed()
        composeTestRule.onNodeWithText("v1.0.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("v1.1.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Faster IMDb matching for TV episodes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download from GitHub").assertIsDisplayed()
    }

    @Test
    fun noReleases_rendersItsCopy() {
        composeTestRule.setContent {
            OpenInStremioTheme {
                UpdateScreen(uiState = UpdateUiState.NoReleases, onBack = {}, onRetry = {})
            }
        }

        composeTestRule.onNodeWithText("No releases published yet.").assertIsDisplayed()
    }

    @Test
    fun errorState_retryInvokesCallback() {
        var retried = false
        composeTestRule.setContent {
            OpenInStremioTheme {
                UpdateScreen(uiState = UpdateUiState.Error, onBack = {}, onRetry = { retried = true })
            }
        }

        composeTestRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }
}
