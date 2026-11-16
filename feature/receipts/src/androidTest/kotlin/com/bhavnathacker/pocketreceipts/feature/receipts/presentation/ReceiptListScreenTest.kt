package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

    private fun setScreen(uiState: ReceiptListUiState) {
        composeRule.setContent {
            PocketReceiptsTheme {
                ReceiptListScreen(
                    uiState = uiState,
                    onReceiptClick = { clickedReceipt = it },
                    onAddClick = { addClicks++ },
                )
            }
        }
    }

    @Test
    fun test_that_the_empty_state_shows_a_message_and_the_add_action() {
        setScreen(ReceiptListUiState.Empty)

        composeRule.onNodeWithText("No receipts yet").assertIsDisplayed()
        composeRule.onNodeWithText("Add receipt").assertIsDisplayed().performClick()
        assertThat(addClicks).isEqualTo(1)
    }

    @Test
    fun test_that_a_row_shows_merchant_date_category_and_total() {
        setScreen(
            ReceiptListUiState.Receipts(
                listOf(ReceiptListItemUiModel(7, "Corner Shop", "3 Mar 2026", "₹1,23,456.50", R.string.category_groceries)),
            ),
        )

        composeRule.onNodeWithText("Corner Shop").assertIsDisplayed()
        composeRule.onNodeWithText("3 Mar 2026 · Groceries").assertIsDisplayed()
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
}
