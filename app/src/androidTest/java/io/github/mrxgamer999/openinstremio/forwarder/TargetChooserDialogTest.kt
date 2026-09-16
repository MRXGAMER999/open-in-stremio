package io.github.mrxgamer999.openinstremio.forwarder

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TargetChooserDialogTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val bothTargets = listOf(Target.STREMIO, Target.FIREGUY)

    @Test
    fun bothRows_nameWhereTheyGo() {
        composeTestRule.setContent {
            OpenInStremioTheme {
                TargetChooserDialog(targets = bothTargets, onPick = {}, onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText("Open this title in…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open in Stremio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open in Fireguy").assertIsDisplayed()
    }

    @Test
    fun pickingFireguy_firesThatTarget() {
        var picked: Target? = null
        composeTestRule.setContent {
            OpenInStremioTheme {
                TargetChooserDialog(targets = bothTargets, onPick = { picked = it }, onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText("Open in Fireguy").performClick()

        assertEquals(Target.FIREGUY, picked)
    }

    @Test
    fun pickingStremio_firesThatTarget() {
        var picked: Target? = null
        composeTestRule.setContent {
            OpenInStremioTheme {
                TargetChooserDialog(targets = bothTargets, onPick = { picked = it }, onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText("Open in Stremio").performClick()

        assertEquals(Target.STREMIO, picked)
    }

    @Test
    fun notNow_firesDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            OpenInStremioTheme {
                TargetChooserDialog(
                    targets = bothTargets,
                    onPick = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Not now").performClick()

        assertTrue(dismissed)
    }
}
