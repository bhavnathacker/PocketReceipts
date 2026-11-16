package com.bhavnathacker.pocketreceipts

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
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
 * Recreates [MainActivity] the way a rotation does (ViewModels are retained),
 * which `StateRestorationTester` can't emulate for Navigation 3 entries.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReceiptsConfigurationChangeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var receiptRepository: ReceiptRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            receiptRepository.monitorReceipts().first().forEach { receiptRepository.deleteReceipt(it.id) }
            receiptRepository.addReceipt(
                ReceiptInput(
                    merchant = "Corner Shop",
                    purchaseDate = LocalDate.now(),
                    total = Money(450, "USD"),
                    category = ReceiptCategory.GROCERIES,
                    note = null,
                ),
            )
        }
    }

    private fun field(label: String) = composeRule.onNode(hasText(label) and hasSetTextAction())

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the edit form, types a new merchant and total, then recreates the activity. */
    private fun editAndRecreate() {
        waitForText("Corner Shop")
        composeRule.onNodeWithText("Corner Shop").performClick()
        composeRule.onNodeWithContentDescription("Edit receipt").performClick()
        waitForText("Edit receipt")
        field("Merchant").performTextClearance()
        field("Merchant").performTextInput("Corner Market")
        field("Total").performTextClearance()
        field("Total").performTextInput("7.25")

        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun test_that_the_edit_form_stays_open_when_the_activity_is_recreated() {
        editAndRecreate()

        composeRule.onNodeWithText("Edit receipt").assertIsDisplayed()
    }

    @Test
    fun test_that_the_edit_form_keeps_the_typed_merchant_when_the_activity_is_recreated() {
        editAndRecreate()

        composeRule.onNodeWithText("Corner Market").assertIsDisplayed()
    }

    @Test
    fun test_that_the_edit_form_keeps_the_typed_total_when_the_activity_is_recreated() {
        editAndRecreate()

        composeRule.onNodeWithText("7.25").assertIsDisplayed()
    }

    @Test
    fun test_that_receipts_stays_selected_when_the_activity_is_recreated() {
        editAndRecreate()

        composeRule.onNode(
            hasText("Receipts") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
        ).assertIsSelected()
    }
}
