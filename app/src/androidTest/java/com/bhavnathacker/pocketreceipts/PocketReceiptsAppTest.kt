package com.bhavnathacker.pocketreceipts

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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PocketReceiptsAppTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

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

    private fun selectAskTwiceThenBack() {
        setApp()
        navItem("Ask").performClick()
        navItem("Ask").performClick()
        pressBack()
    }

    private fun selectSettingsThenBack() {
        setApp()
        navItem("Settings").performClick()
        pressBack()
    }

    private fun selectSettingsThenRestoreState() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            PocketReceiptsTheme {
                PocketReceiptsApp(windowSizeClass = compact)
            }
        }
        navItem("Settings").performClick()
        restorationTester.emulateSavedInstanceStateRestore()
    }

    @Test
    fun test_that_app_opens_on_the_receipts_screen() {
        setApp()

        heading("Receipts").assertIsDisplayed()
    }

    @Test
    fun test_that_app_opens_with_receipts_selected() {
        setApp()

        navItem("Receipts").assertIsSelected()
    }

    @Test
    fun test_that_app_opens_with_ask_not_selected() {
        setApp()

        navItem("Ask").assertIsNotSelected()
    }

    @Test
    fun test_that_app_opens_with_settings_not_selected() {
        setApp()

        navItem("Settings").assertIsNotSelected()
    }

    @Test
    fun test_that_navigation_shows_receipts_ask_and_settings_in_order() {
        setApp()

        val receipts = navItem("Receipts").getUnclippedBoundsInRoot()
        val ask = navItem("Ask").getUnclippedBoundsInRoot()
        val settings = navItem("Settings").getUnclippedBoundsInRoot()

        assertThat(listOf(receipts.left, ask.left, settings.left)).isInStrictOrder()
    }

    @Test
    fun test_that_selecting_ask_shows_the_ask_screen() {
        setApp()

        navItem("Ask").performClick()

        heading("Ask").assertIsDisplayed()
    }

    @Test
    fun test_that_selecting_ask_marks_ask_selected() {
        setApp()

        navItem("Ask").performClick()

        navItem("Ask").assertIsSelected()
    }

    @Test
    fun test_that_selecting_ask_marks_receipts_not_selected() {
        setApp()

        navItem("Ask").performClick()

        navItem("Receipts").assertIsNotSelected()
    }

    @Test
    fun test_that_back_shows_the_receipts_screen_when_ask_is_reselected() {
        selectAskTwiceThenBack()

        heading("Receipts").assertIsDisplayed()
    }

    @Test
    fun test_that_back_selects_receipts_when_ask_is_reselected() {
        selectAskTwiceThenBack()

        navItem("Receipts").assertIsSelected()
    }

    @Test
    fun test_that_back_from_settings_shows_the_receipts_screen() {
        selectSettingsThenBack()

        heading("Receipts").assertIsDisplayed()
    }

    @Test
    fun test_that_back_from_settings_selects_receipts() {
        selectSettingsThenBack()

        navItem("Receipts").assertIsSelected()
    }

    @Test
    fun test_that_back_from_settings_closes_the_settings_screen() {
        selectSettingsThenBack()

        // Only the navigation item is left.
        composeRule.onAllNodesWithText("Settings").assertCountEquals(1)
    }

    @Test
    fun test_that_the_selected_screen_survives_state_restoration() {
        selectSettingsThenRestoreState()

        heading("Settings").assertIsDisplayed()
    }

    @Test
    fun test_that_the_selected_navigation_item_survives_state_restoration() {
        selectSettingsThenRestoreState()

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
