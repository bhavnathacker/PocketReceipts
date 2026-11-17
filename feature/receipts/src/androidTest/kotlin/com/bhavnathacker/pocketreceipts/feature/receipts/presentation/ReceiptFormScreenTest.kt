package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.MoneyFormatter
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptFormUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import de.palm.composestateevents.triggered
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ReceiptFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 3, 3)
    private val emptyForm = ReceiptFormUiState(
        isEditing = false,
        isLoading = false,
        today = today,
        currencyCode = "EUR",
        purchaseDate = today,
    )

    private var saves = 0
    private var closes = 0
    private var merchant: String? = null
    private var amount: String? = null
    private var category: ReceiptCategory? = null

    private fun setScreen(initialState: ReceiptFormUiState) {
        composeRule.setContent {
            // Feed typed text back into the state, as the ViewModel does.
            var uiState by remember { mutableStateOf(initialState) }
            PocketReceiptsTheme {
                ReceiptFormScreen(
                    uiState = uiState,
                    onClose = { closes++ },
                    onSave = { saves++ },
                    onMerchantChange = {
                        merchant = it
                        uiState = uiState.copy(merchant = it)
                    },
                    onAmountChange = {
                        amount = it
                        uiState = uiState.copy(amount = it)
                    },
                    onCurrencyChange = {},
                    onDateChange = {},
                    onCategoryChange = { category = it },
                    onNoteChange = {},
                )
            }
        }
    }

    @Test
    fun test_that_a_new_receipt_form_is_titled_add_receipt() {
        setScreen(emptyForm)

        composeRule.onNodeWithText("Add receipt").assertIsDisplayed()
    }

    @Test
    fun test_that_a_new_receipt_form_shows_the_currency() {
        setScreen(emptyForm)

        composeRule.onNodeWithText("EUR").assertIsDisplayed()
    }

    @Test
    fun test_that_a_new_receipt_form_shows_the_purchase_date() {
        setScreen(emptyForm)

        composeRule.onNodeWithText("Mar 3, 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun test_that_a_new_receipt_form_shows_the_category() {
        setScreen(emptyForm)

        composeRule.onNodeWithText("Other").assertIsDisplayed()
    }

    @Test
    fun test_that_the_edit_form_is_titled_edit_receipt() {
        setScreen(emptyForm.copy(isEditing = true))

        composeRule.onNodeWithText("Edit receipt").assertIsDisplayed()
    }

    @Test
    fun test_that_the_edit_form_shows_the_merchant() {
        setScreen(emptyForm.copy(isEditing = true, merchant = "Corner Shop"))

        composeRule.onNodeWithText("Corner Shop").assertIsDisplayed()
    }

    @Test
    fun test_that_typing_a_merchant_reports_it() {
        setScreen(emptyForm)

        composeRule.onNode(hasText("Merchant") and hasSetTextAction()).performTextInput("Corner Shop")

        assertThat(merchant).isEqualTo("Corner Shop")
    }

    @Test
    fun test_that_typing_an_amount_reports_it() {
        setScreen(emptyForm)

        composeRule.onNode(hasText("Total") and hasSetTextAction()).performTextInput("4.50")

        assertThat(amount).isEqualTo("4.50")
    }

    @Test
    fun test_that_save_reports_the_save_action() {
        setScreen(emptyForm)

        composeRule.onNodeWithText("Save").performClick()

        assertThat(saves).isEqualTo(1)
    }

    @Test
    fun test_that_close_reports_the_close_action() {
        setScreen(emptyForm)

        composeRule.onNodeWithContentDescription("Close").performClick()

        assertThat(closes).isEqualTo(1)
    }

    @Test
    fun test_that_the_merchant_error_is_shown() {
        setScreen(emptyForm.copy(merchantError = R.string.receipt_form_error_merchant_blank))

        composeRule.onNodeWithText("Enter a merchant").assertIsDisplayed()
    }

    @Test
    fun test_that_the_amount_error_is_shown() {
        setScreen(emptyForm.copy(amountError = R.string.receipt_form_error_amount_invalid))

        composeRule.onNodeWithText("Enter a valid amount").assertIsDisplayed()
    }

    @Test
    fun test_that_the_note_error_is_shown() {
        setScreen(emptyForm.copy(noteError = R.string.receipt_form_error_note_too_long))

        composeRule.onNodeWithText("Use 500 characters or fewer").assertExists()
    }

    @Test
    fun test_that_save_is_disabled_while_saving() {
        setScreen(emptyForm.copy(isSaving = true))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun test_that_choosing_a_category_reports_it() {
        setScreen(emptyForm)

        composeRule.onNodeWithText("Other").performClick()
        composeRule.onNodeWithText("Dining").performClick()

        assertThat(category).isEqualTo(ReceiptCategory.DINING)
    }

    @Test
    fun test_that_searching_currencies_shows_matching_currencies() {
        setScreen(emptyForm)
        composeRule.onNodeWithText("EUR").performClick()

        composeRule.onNode(hasText("Search currencies") and hasSetTextAction()).performTextInput("rupee")

        composeRule.onNodeWithText("INR", substring = true).assertIsDisplayed()
    }

    private val reviewForm = emptyForm.copy(
        isReview = true,
        merchant = "Corner Shop",
        amount = "210.00",
        currencyCode = "GBP",
        purchaseDate = LocalDate.of(2026, 2, 3),
        dateCandidates = listOf(LocalDate.of(2026, 2, 3), LocalDate.of(2026, 1, 30)),
        totalCandidates = listOf(Money(21000, "GBP"), Money(20000, "GBP")),
    )

    private var pickedDate: LocalDate? = null
    private var pickedTotal: Money? = null

    private fun setReviewScreen(uiState: ReceiptFormUiState = reviewForm) {
        composeRule.setContent {
            PocketReceiptsTheme {
                ReceiptFormScreen(
                    uiState = uiState,
                    onClose = {},
                    onSave = {},
                    onMerchantChange = {},
                    onAmountChange = {},
                    onCurrencyChange = {},
                    onDateChange = {},
                    onCategoryChange = {},
                    onNoteChange = {},
                    onDateCandidateClick = { pickedDate = it },
                    onTotalCandidateClick = { pickedTotal = it },
                )
            }
        }
    }

    private fun totalLabel(money: Money) = MoneyFormatter()(money, Locale.getDefault())

    private fun dateLabel(date: LocalDate) =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)

    @Test
    fun test_that_a_scanned_receipt_form_is_titled_review_receipt() {
        setReviewScreen()

        composeRule.onNodeWithText("Review receipt").assertIsDisplayed()
    }

    @Test
    fun test_that_the_total_in_the_form_is_the_selected_candidate() {
        setReviewScreen()

        composeRule.onNodeWithContentDescription("Use total ${totalLabel(Money(21000, "GBP"))}").assertIsSelected()
    }

    @Test
    fun test_that_tapping_a_total_candidate_reports_it() {
        setReviewScreen()

        composeRule.onNodeWithContentDescription("Use total ${totalLabel(Money(20000, "GBP"))}").performClick()

        assertThat(pickedTotal).isEqualTo(Money(20000, "GBP"))
    }

    @Test
    fun test_that_the_date_in_the_form_is_the_selected_candidate() {
        setReviewScreen()

        composeRule.onNodeWithContentDescription("Use date ${dateLabel(LocalDate.of(2026, 2, 3))}").assertIsSelected()
    }

    @Test
    fun test_that_tapping_a_date_candidate_reports_it() {
        setReviewScreen()

        composeRule.onNodeWithContentDescription("Use date ${dateLabel(LocalDate.of(2026, 1, 30))}").performClick()

        assertThat(pickedDate).isEqualTo(LocalDate.of(2026, 1, 30))
    }

    @Test
    fun test_that_a_manual_form_shows_no_candidates() {
        setScreen(emptyForm)

        composeRule.onNodeWithContentDescription("Use total", substring = true).assertDoesNotExist()
    }

    @Test
    fun test_that_an_empty_scan_shows_the_nothing_recognized_message() {
        setReviewScreen(emptyForm.copy(isReview = true, nothingRecognizedEvent = triggered))

        composeRule.onNodeWithText("Nothing could be read from this scan").assertIsDisplayed()
    }
}
