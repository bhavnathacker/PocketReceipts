package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.core.ui.R as CoreUiR
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiModel
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val loaded = ReceiptDetailUiState(
        isLoading = false,
        receipt = ReceiptDetailUiModel(
            merchant = "Corner Shop",
            date = "3 Mar 2026",
            total = "£4.50",
            categoryLabel = CoreUiR.string.category_groceries,
            note = "milk",
        ),
    )

    private var edits = 0
    private var deletesConfirmed = 0
    private var backs = 0
    private var deleteDismisses = 0

    /** Hosts the screen with the confirmation dialog state handled as the ViewModel does. */
    private fun setScreen(initialState: ReceiptDetailUiState = loaded) {
        composeRule.setContent {
            var uiState by remember { mutableStateOf(initialState) }
            PocketReceiptsTheme {
                ReceiptDetailScreen(
                    uiState = uiState,
                    onBack = { backs++ },
                    onEdit = { edits++ },
                    onDeleteClick = { uiState = uiState.copy(showDeleteConfirmation = true) },
                    onDeleteConfirm = {
                        deletesConfirmed++
                        uiState = uiState.copy(showDeleteConfirmation = false)
                    },
                    onDeleteDismiss = {
                        deleteDismisses++
                        uiState = uiState.copy(showDeleteConfirmation = false)
                    },
                )
            }
        }
    }

    @Test
    fun test_that_the_merchant_is_shown() {
        setScreen()

        composeRule.onNodeWithText("Corner Shop").assertIsDisplayed()
    }

    @Test
    fun test_that_the_total_is_shown() {
        setScreen()

        composeRule.onNodeWithText("£4.50").assertIsDisplayed()
    }

    @Test
    fun test_that_the_date_is_shown() {
        setScreen()

        composeRule.onNodeWithText("3 Mar 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun test_that_the_category_is_shown() {
        setScreen()

        composeRule.onNodeWithText("Groceries", substring = true).assertIsDisplayed()
    }

    @Test
    fun test_that_the_note_is_shown() {
        setScreen()

        composeRule.onNodeWithText("milk", substring = true).assertIsDisplayed()
    }

    @Test
    fun test_that_edit_reports_the_edit_action() {
        setScreen()

        composeRule.onNodeWithContentDescription("Edit receipt").performClick()

        assertThat(edits).isEqualTo(1)
    }

    @Test
    fun test_that_back_reports_the_back_action() {
        setScreen()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun test_that_delete_shows_the_confirmation() {
        setScreen()

        composeRule.onNodeWithContentDescription("Delete receipt").performClick()

        composeRule.onNodeWithText("Delete this receipt?").assertIsDisplayed()
    }

    @Test
    fun test_that_confirming_delete_reports_the_delete() {
        setScreen()
        composeRule.onNodeWithContentDescription("Delete receipt").performClick()

        composeRule.onNodeWithText("Delete").performClick()

        assertThat(deletesConfirmed).isEqualTo(1)
    }

    @Test
    fun test_that_cancelling_delete_reports_the_dismiss() {
        setScreen()
        composeRule.onNodeWithContentDescription("Delete receipt").performClick()

        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(deleteDismisses).isEqualTo(1)
    }

    @Test
    fun test_that_cancelling_delete_does_not_report_a_delete() {
        setScreen()
        composeRule.onNodeWithContentDescription("Delete receipt").performClick()

        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(deletesConfirmed).isEqualTo(0)
    }
}
