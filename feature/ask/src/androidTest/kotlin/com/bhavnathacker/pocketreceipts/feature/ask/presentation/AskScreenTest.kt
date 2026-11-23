package com.bhavnathacker.pocketreceipts.feature.ask.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.core.ui.R as CoreUiR
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.ask.R
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskAnswerUiModel
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskTotalUiModel
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AskScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var asks = 0

    private fun setScreen(uiState: AskUiState) {
        composeRule.setContent {
            PocketReceiptsTheme {
                AskScreen(uiState = uiState, onQuestionChange = {}, onExampleSelected = {}, onAsk = { asks++ })
            }
        }
    }

    @Test
    fun test_that_ask_is_disabled_while_the_question_is_blank() {
        setScreen(AskUiState(question = " "))

        composeRule.onNode(hasText("Ask") and hasClickAction() and !hasSetTextAction()).assertIsNotEnabled()
    }

    @Test
    fun test_that_ask_reports_the_click_when_there_is_a_question() {
        setScreen(AskUiState(question = "How much in 2025?"))

        composeRule.onNode(hasText("Ask") and hasClickAction() and !hasSetTextAction()).assertIsEnabled().performClick()

        assertThat(asks).isEqualTo(1)
    }

    @Test
    fun test_that_tapping_an_example_fills_the_question() {
        composeRule.setContent {
            var state by remember { mutableStateOf(AskUiState()) }
            PocketReceiptsTheme {
                AskScreen(
                    uiState = state,
                    onQuestionChange = { state = state.copy(question = it) },
                    onExampleSelected = { state = state.copy(question = it) },
                    onAsk = {},
                )
            }
        }

        composeRule.onNodeWithText("How much did I spend on shopping this quarter?").performScrollTo().performClick()

        composeRule.onNode(hasSetTextAction() and hasText("How much did I spend on shopping this quarter?"))
            .assertIsDisplayed()
        composeRule.onNode(hasText("Ask") and hasClickAction() and !hasSetTextAction()).assertIsEnabled()
    }

    @Test
    fun test_that_an_answer_shows_category_period_amount_and_count() {
        setScreen(
            AskUiState(
                question = "How much did I spend on groceries in October?",
                answer = AskAnswerUiModel.Totals(
                    categoryLabel = CoreUiR.string.category_groceries,
                    period = "1 – 31 Oct 2026",
                    totals = listOf(AskTotalUiModel("₹200.00", 2), AskTotalUiModel("€12.30", 1)),
                ),
            ),
        )

        composeRule.onNodeWithText("Groceries", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("1 – 31 Oct 2026", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("₹200.00", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("2 receipts", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("€12.30", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("1 receipt", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun test_that_all_spending_over_all_time_is_labelled() {
        setScreen(AskUiState(answer = AskAnswerUiModel.NothingSpent(R.string.ask_all_spending, period = null)))

        composeRule.onNodeWithText("All spending", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("All time", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("No receipts found for this category and period.", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun test_that_an_unanswerable_question_shows_the_message_and_examples() {
        setScreen(AskUiState(question = "Which shop did I visit most?", answer = AskAnswerUiModel.Unanswerable))

        composeRule.onNodeWithText("I can only answer", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("How much did I spend on groceries in October?").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test_that_answering_shows_progress() {
        setScreen(AskUiState(question = "How much in 2025?", isAnswering = true))

        composeRule.onNodeWithText("Reading your question…").assertIsDisplayed()
        composeRule.onNode(hasText("Ask") and hasClickAction() and !hasSetTextAction()).assertIsNotEnabled()
    }

    @Test
    fun test_that_a_failure_shows_an_error_and_keeps_the_question() {
        setScreen(AskUiState(question = "How much in 2025?", hasError = true))

        composeRule.onNodeWithText("Couldn't answer the question. Try again.").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText("How much in 2025?")).assertIsDisplayed()
    }
}
