package io.github.mrxgamer999.openinstremio.forwarder

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StremioMissingDialogTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bothActions_exist() {
        composeTestRule.setContent {
            OpenInStremioTheme { StremioMissingDialog(onGetStremio = {}, onDismiss = {}) }
        }

        composeTestRule.onNodeWithText("Stremio isn’t installed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Not now").assertIsDisplayed()
        // Note: the confirm button also requests initial focus for D-pad devices, but focus
        // is only granted in key-input mode (TV) - touch-mode emulators reject it, so it is
        // verified on the Android TV image instead of asserted here.
        composeTestRule.onNodeWithText("Get Stremio on Google Play").assertIsDisplayed()
    }

    @Test
    fun notNow_firesDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            OpenInStremioTheme {
                StremioMissingDialog(onGetStremio = {}, onDismiss = { dismissed = true })
            }
        }

        composeTestRule.onNodeWithText("Not now").performClick()

        assertTrue(dismissed)
    }
}
