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
            categoryLabel = R.string.category_groceries,
            note = "milk",
        ),
    )

    private var edits = 0
    private var deletesConfirmed = 0
    private var backs = 0

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
                    onDeleteDismiss = { uiState = uiState.copy(showDeleteConfirmation = false) },
                )
            }
        }
    }

    @Test
    fun test_that_all_fields_are_shown() {
        setScreen()

        composeRule.onNodeWithText("Corner Shop").assertIsDisplayed()
        composeRule.onNodeWithText("£4.50").assertIsDisplayed()
        composeRule.onNodeWithText("3 Mar 2026", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Groceries", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("milk", substring = true).assertIsDisplayed()
    }

    @Test
    fun test_that_edit_and_back_report_their_actions() {
        setScreen()

        composeRule.onNodeWithContentDescription("Edit receipt").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(edits).isEqualTo(1)
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun test_that_confirming_delete_deletes() {
        setScreen()

        composeRule.onNodeWithContentDescription("Delete receipt").performClick()
        composeRule.onNodeWithText("Delete this receipt?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()

        assertThat(deletesConfirmed).isEqualTo(1)
        composeRule.onNodeWithText("Delete this receipt?").assertDoesNotExist()
    }

    @Test
    fun test_that_cancelling_delete_keeps_the_receipt() {
        setScreen()

        composeRule.onNodeWithContentDescription("Delete receipt").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(deletesConfirmed).isEqualTo(0)
        composeRule.onNodeWithText("Delete this receipt?").assertDoesNotExist()
        composeRule.onNodeWithText("Corner Shop").assertIsDisplayed()
    }
}
