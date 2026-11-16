package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListItemUiModel
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var clickedReceipt: Long? = null
    private var addClicks = 0
    private var scanClicks = 0

    private fun setScreen(uiState: ReceiptListUiState) {
        composeRule.setContent {
            PocketReceiptsTheme {
                ReceiptListScreen(
                    uiState = uiState,
                    onReceiptClick = { clickedReceipt = it },
                    onAddClick = { addClicks++ },
                    onScanClick = { scanClicks++ },
                )
            }
        }
    }

    private val cornerShop = ReceiptListUiState.Receipts(
        listOf(ReceiptListItemUiModel(7, "Corner Shop", "3 Mar 2026", "₹1,23,456.50", R.string.category_groceries)),
    )

    @Test
    fun test_that_the_empty_state_shows_a_message() {
        setScreen(ReceiptListUiState.Empty)

        composeRule.onNodeWithText("No receipts yet").assertIsDisplayed()
    }

    @Test
    fun test_that_the_add_action_reports_the_click() {
        setScreen(ReceiptListUiState.Empty)

        composeRule.onNodeWithText("Add receipt").performClick()

        assertThat(addClicks).isEqualTo(1)
    }

    @Test
    fun test_that_a_row_shows_the_merchant() {
        setScreen(cornerShop)

        composeRule.onNodeWithText("Corner Shop").assertIsDisplayed()
    }

    @Test
    fun test_that_a_row_shows_the_date_and_category() {
        setScreen(cornerShop)

        composeRule.onNodeWithText("3 Mar 2026 · Groceries").assertIsDisplayed()
    }

    @Test
    fun test_that_a_row_shows_the_total() {
        setScreen(cornerShop)

        composeRule.onNodeWithText("₹1,23,456.50").assertIsDisplayed()
    }

    @Test
    fun test_that_selecting_a_row_reports_its_receipt_id() {
        setScreen(
            ReceiptListUiState.Receipts(
                listOf(
                    ReceiptListItemUiModel(7, "Corner Shop", "3 Mar 2026", "€4.50", R.string.category_groceries),
                    ReceiptListItemUiModel(8, "Ramen Ya", "2 Mar 2026", "¥1,500", R.string.category_dining),
                ),
            ),
        )

        composeRule.onNodeWithText("Ramen Ya").performClick()

        assertThat(clickedReceipt).isEqualTo(8)
    }

    @Test
    fun test_that_the_error_state_shows_a_message() {
        setScreen(ReceiptListUiState.Error)

        composeRule.onNodeWithText("Your receipts couldn't be opened.").assertIsDisplayed()
    }

    @Test
    fun test_that_the_empty_state_scan_action_reports_the_click() {
        setScreen(ReceiptListUiState.Empty)

        composeRule.onNodeWithText("Scan receipt").performClick()

        assertThat(scanClicks).isEqualTo(1)
    }

    @Test
    fun test_that_the_scan_button_reports_the_click_when_receipts_exist() {
        setScreen(cornerShop)

        composeRule.onNodeWithText("Scan receipt").performClick()

        assertThat(scanClicks).isEqualTo(1)
    }

    @Test
    fun test_that_the_top_bar_add_action_reports_the_click() {
        setScreen(cornerShop)

        composeRule.onNodeWithContentDescription("Add receipt").performClick()

        assertThat(addClicks).isEqualTo(1)
    }
}
