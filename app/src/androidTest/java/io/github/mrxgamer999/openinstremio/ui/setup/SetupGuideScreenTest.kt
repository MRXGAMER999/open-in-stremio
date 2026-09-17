package io.github.mrxgamer999.openinstremio.ui.setup

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.mrxgamer999.openinstremio.data.PackageChecker
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupGuideScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val uiState = setupGuideState(PlayerChoice.BOTH, PackageChecker { false })

    @Test
    fun firstPage_isVisible() {
        composeTestRule.setContent {
            OpenInStremioTheme { SetupGuideScreen(uiState = uiState, onBack = {}, onChoose = {}) }
        }

        composeTestRule.onNodeWithText("What this does").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun steppingThroughAllPages_reachesFinish_andFinishInvokesBack() {
        var backCalled = false
        composeTestRule.setContent {
            OpenInStremioTheme { SetupGuideScreen(uiState = uiState, onBack = { backCalled = true }, onChoose = {}) }
        }

        repeat(4) { composeTestRule.onNodeWithText("Next").performClick() }

        composeTestRule.onNodeWithText("You’re all set").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finish setup").assertIsDisplayed().performClick()
        assertTrue(backCalled)
    }

    @Test
    fun secondPage_asksWhichApps_andNextKeepsTheShownChoice() {
        val chosen = mutableListOf<PlayerChoice>()
        composeTestRule.setContent {
            OpenInStremioTheme { SetupGuideScreen(uiState = uiState, onBack = {}, onChoose = { chosen += it }) }
        }

        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Which apps do you use?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fireguy").performClick()
        assertEquals(listOf(PlayerChoice.FIREGUY), chosen)

        // The state is fixed at both here, so Next records what the page shows.
        composeTestRule.onNodeWithText("Next").performClick()
        assertEquals(listOf(PlayerChoice.FIREGUY, PlayerChoice.BOTH), chosen)
    }

    @Test
    fun thirdPage_hasSeriesGuideButton() {
        composeTestRule.setContent {
            OpenInStremioTheme { SetupGuideScreen(uiState = uiState, onBack = {}, onChoose = {}) }
        }

        repeat(2) { composeTestRule.onNodeWithText("Next").performClick() }

        composeTestRule.onNodeWithText("Open SeriesGuide → Extensions").assertIsDisplayed()
    }
}
