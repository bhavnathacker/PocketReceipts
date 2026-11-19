package com.bhavnathacker.pocketreceipts

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ReceiptsKey
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.receiptReviewKey
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
 * The review form for a scanned receipt, opened directly on the back stack: the
 * real document scanner runs in Google Play services and can't be driven here.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReceiptReviewFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var receiptRepository: ReceiptRepository

    private val compact = WindowSizeClass(minWidthDp = 0, minHeightDp = 0)
    private val scan = ReceiptScan(
        merchant = "Scanned Shop",
        date = LocalDate.now(),
        total = Money(450, "USD"),
    )

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            receiptRepository.monitorReceipts().first().forEach { receiptRepository.deleteReceipt(it.id) }
        }
        composeRule.setContent {
            PocketReceiptsTheme {
                PocketReceiptsApp(
                    windowSizeClass = compact,
                    initialBackStack = listOf(ReceiptsKey, receiptReviewKey(scan)),
                )
            }
        }
    }

    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun test_that_receipts_stays_selected_on_the_review_form() {
        composeRule.onNodeWithText("Review receipt").assertIsDisplayed()

        composeRule.onNode(
            hasText("Receipts") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
        ).assertIsSelected()
    }

    @Test
    fun test_that_back_from_the_review_form_shows_the_receipts_list() {
        pressBack()

        composeRule.onNode(hasText("Receipts") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun test_that_back_from_the_review_form_saves_nothing() {
        pressBack()

        composeRule.onNode(hasText("Receipts") and isHeading()).assertIsDisplayed()
        assertThat(runBlocking { receiptRepository.monitorReceipts().first() }).isEmpty()
    }

    @Test
    fun test_that_saving_from_the_review_form_adds_the_receipt() {
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Receipts") and isHeading()).fetchSemanticsNodes().isNotEmpty()
        }
        val stored = runBlocking { receiptRepository.monitorReceipts().first() }
        assertThat(stored.map { it.merchant to it.total }).containsExactly("Scanned Shop" to Money(450, "USD"))
    }
}
