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

    @Test
    fun test_that_the_edit_form_keeps_typed_input_when_the_activity_is_recreated() {
        waitForText("Corner Shop")
        composeRule.onNodeWithText("Corner Shop").performClick()
        composeRule.onNodeWithContentDescription("Edit receipt").performClick()
        waitForText("Edit receipt")
        field("Merchant").performTextClearance()
        field("Merchant").performTextInput("Corner Market")
        field("Total").performTextClearance()
        field("Total").performTextInput("7.25")

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Edit receipt").assertIsDisplayed()
        composeRule.onNodeWithText("Corner Market").assertIsDisplayed()
        composeRule.onNodeWithText("7.25").assertIsDisplayed()
        composeRule.onNode(
            hasText("Receipts") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
        ).assertIsSelected()
    }
}
