package com.bhavnathacker.pocketreceipts

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import javax.inject.Inject

/**
 * Receipt flows through the whole app, against the real database.
 * Each test starts with no receipts.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReceiptsFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var receiptRepository: ReceiptRepository

    private val compact = WindowSizeClass(minWidthDp = 0, minHeightDp = 0)

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            receiptRepository.monitorReceipts().first().forEach { receiptRepository.deleteReceipt(it.id) }
        }
    }

    private fun setApp() {
        composeRule.setContent {
            PocketReceiptsTheme {
                PocketReceiptsApp(windowSizeClass = compact)
            }
        }
    }

    private fun addReceipt(merchant: String = "Corner Shop", minorUnits: Long = 450): Long = runBlocking {
        receiptRepository.addReceipt(
            ReceiptInput(
                merchant = merchant,
                purchaseDate = LocalDate.now(),
                total = Money(minorUnits, "USD"),
                category = ReceiptCategory.GROCERIES,
                note = null,
            ),
        )
    }

    private fun navItem(label: String) = composeRule.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
    )

    private fun field(label: String) = composeRule.onNode(hasText(label) and hasSetTextAction())

    /** Waits for Room writes and reads, which happen off the UI thread. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForNoText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun storedReceipts() = runBlocking { receiptRepository.monitorReceipts().first() }

    /** Opens the detail screen of the seeded receipt and switches to Ask. */
    private fun openDetailThenSwitchToAsk() {
        addReceipt()
        setApp()
        waitForText("Corner Shop")
        composeRule.onNodeWithText("Corner Shop").performClick()
        composeRule.onNodeWithContentDescription("Edit receipt").assertIsDisplayed()
        navItem("Ask").performClick()
    }

    private fun openAddFormWithoutMerchant() {
        setApp()
        waitForText("No receipts yet")
        composeRule.onNodeWithText("Add receipt").performClick()
        field("Total").performTextInput("4.50")
    }

    private fun openDetailAndConfirmDelete() {
        addReceipt()
        setApp()
        waitForText("Corner Shop")
        composeRule.onNodeWithText("Corner Shop").performClick()
        composeRule.onNodeWithContentDescription("Delete receipt").performClick()
        composeRule.onNodeWithText("Delete").performClick()
    }

    @Test
    fun test_that_receipts_stays_selected_on_the_detail_screen() {
        addReceipt()
        setApp()
        waitForText("Corner Shop")

        composeRule.onNodeWithText("Corner Shop").performClick()

        navItem("Receipts").assertIsSelected()
    }

    @Test
    fun test_that_back_after_switching_tabs_from_detail_shows_the_receipts_list() {
        openDetailThenSwitchToAsk()

        pressBack()

        composeRule.onNode(hasText("Receipts") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun test_that_back_after_switching_tabs_from_detail_does_not_show_the_detail_screen() {
        openDetailThenSwitchToAsk()

        pressBack()

        composeRule.onNodeWithContentDescription("Edit receipt").assertDoesNotExist()
    }

    @Test
    fun test_that_adding_a_receipt_shows_it_in_the_list() {
        setApp()
        waitForText("No receipts yet")
        composeRule.onNodeWithText("Add receipt").performClick()
        field("Merchant").performTextInput("  Corner Shop  ")
        field("Total").performTextInput("4.50")

        composeRule.onNodeWithText("Save").performClick()

        waitForText("$4.50")
        composeRule.onNodeWithText("Corner Shop").assertIsDisplayed()
    }

    @Test
    fun test_that_editing_a_receipt_shows_the_new_total_on_the_detail_screen() {
        addReceipt()
        setApp()
        waitForText("Corner Shop")
        composeRule.onNodeWithText("Corner Shop").performClick()
        composeRule.onNodeWithContentDescription("Edit receipt").performClick()
        waitForText("Edit receipt")
        field("Total").performTextClearance()
        field("Total").performTextInput("5.00")

        composeRule.onNodeWithText("Save").performClick()

        waitForText("$5.00")
        composeRule.onNodeWithContentDescription("Delete receipt").assertIsDisplayed()
    }

    @Test
    fun test_that_deleting_the_only_receipt_shows_the_empty_list() {
        openDetailAndConfirmDelete()

        waitForText("No receipts yet")
    }

    @Test
    fun test_that_deleting_a_receipt_removes_it_from_storage() {
        openDetailAndConfirmDelete()

        waitForText("No receipts yet")
        assertThat(storedReceipts()).isEmpty()
    }

    @Test
    fun test_that_saving_without_a_merchant_shows_an_error() {
        openAddFormWithoutMerchant()

        composeRule.onNodeWithText("Save").performClick()

        waitForText("Enter a merchant")
    }

    @Test
    fun test_that_saving_without_a_merchant_saves_nothing() {
        openAddFormWithoutMerchant()

        composeRule.onNodeWithText("Save").performClick()

        waitForText("Enter a merchant")
        assertThat(storedReceipts()).isEmpty()
    }
}
