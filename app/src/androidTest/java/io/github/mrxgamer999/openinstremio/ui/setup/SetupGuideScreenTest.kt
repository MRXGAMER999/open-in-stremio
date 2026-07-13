package io.github.mrxgamer999.openinstremio.ui.setup

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupGuideScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val uiState = SetupGuideViewModel().uiState.value

    @Test
    fun firstPage_isVisible() {
        composeTestRule.setContent {
            OpenInStremioTheme { SetupGuideScreen(uiState = uiState, onBack = {}) }
        }

        composeTestRule.onNodeWithText("What this does").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun steppingThroughAllPages_reachesFinish_andFinishInvokesBack() {
        var backCalled = false
        composeTestRule.setContent {
            OpenInStremioTheme { SetupGuideScreen(uiState = uiState, onBack = { backCalled = true }) }
        }

        repeat(3) { composeTestRule.onNodeWithText("Next").performClick() }

        composeTestRule.onNodeWithText("You’re all set").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finish setup").assertIsDisplayed().performClick()
        assertTrue(backCalled)
    }

    @Test
    fun secondPage_hasSeriesGuideButton() {
        composeTestRule.setContent {
            OpenInStremioTheme { SetupGuideScreen(uiState = uiState, onBack = {}) }
        }

        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.onNodeWithText("Open SeriesGuide → Extensions").assertIsDisplayed()
    }
}
