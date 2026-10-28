package com.bhavnathacker.pocketreceipts

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PocketReceiptsAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val compact = WindowSizeClass(minWidthDp = 0, minHeightDp = 0)
    private val medium = WindowSizeClass(
        minWidthDp = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
        minHeightDp = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )

    private fun setApp(windowSizeClass: WindowSizeClass = compact) {
        composeRule.setContent {
            PocketReceiptsTheme {
                PocketReceiptsApp(windowSizeClass = windowSizeClass)
            }
        }
    }

    private fun navItem(label: String) = composeRule.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
    )

    private fun heading(title: String) = composeRule.onNode(hasText(title) and isHeading())

    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun test_that_app_opens_on_receipts_with_receipts_selected() {
        setApp()

        heading("Receipts").assertIsDisplayed()
        navItem("Receipts").assertIsSelected()
        navItem("Ask").assertIsNotSelected()
        navItem("Settings").assertIsNotSelected()
    }

    @Test
    fun test_that_navigation_shows_receipts_ask_and_settings_in_order() {
        setApp()

        val receipts = navItem("Receipts").getUnclippedBoundsInRoot()
        val ask = navItem("Ask").getUnclippedBoundsInRoot()
        val settings = navItem("Settings").getUnclippedBoundsInRoot()

        assertThat(receipts.left).isLessThan(ask.left)
        assertThat(ask.left).isLessThan(settings.left)
    }

    @Test
    fun test_that_selecting_ask_shows_ask_and_marks_it_selected() {
        setApp()

        navItem("Ask").performClick()

        heading("Ask").assertIsDisplayed()
        navItem("Ask").assertIsSelected()
        navItem("Receipts").assertIsNotSelected()
    }

    @Test
    fun test_that_back_returns_to_receipts_once_when_ask_is_reselected() {
        setApp()
        navItem("Ask").performClick()
        navItem("Ask").performClick()

        pressBack()

        heading("Receipts").assertIsDisplayed()
        navItem("Receipts").assertIsSelected()
    }

    @Test
    fun test_that_back_from_settings_returns_to_receipts() {
        setApp()
        navItem("Settings").performClick()

        pressBack()

        heading("Receipts").assertIsDisplayed()
        navItem("Receipts").assertIsSelected()
        composeRule.onAllNodesWithText("Settings").assertCountEquals(1)
    }

    @Test
    fun test_that_selected_destination_survives_state_restoration() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            PocketReceiptsTheme {
                PocketReceiptsApp(windowSizeClass = compact)
            }
        }
        navItem("Settings").performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        heading("Settings").assertIsDisplayed()
        navItem("Settings").assertIsSelected()
    }

    @Test
    fun test_that_compact_width_shows_a_navigation_bar() {
        setApp(windowSizeClass = compact)

        val receipts = navItem("Receipts").getUnclippedBoundsInRoot()
        val ask = navItem("Ask").getUnclippedBoundsInRoot()

        // Bar: items side by side in one row.
        assertThat(ask.left).isGreaterThan(receipts.left)
        assertThat(ask.top).isEqualTo(receipts.top)
    }

    @Test
    fun test_that_medium_width_shows_a_navigation_rail() {
        setApp(windowSizeClass = medium)

        val receipts = navItem("Receipts").getUnclippedBoundsInRoot()
        val ask = navItem("Ask").getUnclippedBoundsInRoot()

        // Rail: items stacked in one column.
        assertThat(ask.top).isGreaterThan(receipts.top)
        assertThat(ask.left).isEqualTo(receipts.left)
    }
}
