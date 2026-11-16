package com.bhavnathacker.pocketreceipts.feature.capture.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.capture.presentation.model.CaptureStatus
import com.bhavnathacker.pocketreceipts.feature.capture.presentation.model.CaptureUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var cancels = 0
    private var retries = 0
    private var manualAdds = 0

    private fun setScreen(status: CaptureStatus) {
        composeRule.setContent {
            PocketReceiptsTheme {
                CaptureScreen(
                    uiState = CaptureUiState(status = status),
                    onCancel = { cancels++ },
                    onRetry = { retries++ },
                    onAddManually = { manualAdds++ },
                )
            }
        }
    }

    @Test
    fun test_that_reading_shows_progress_text() {
        setScreen(CaptureStatus.Reading)

        composeRule.onNodeWithText("Reading receipt…").assertIsDisplayed()
    }

    @Test
    fun test_that_cancel_while_reading_calls_onCancel() {
        setScreen(CaptureStatus.Reading)

        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(cancels).isEqualTo(1)
    }

    @Test
    fun test_that_unavailable_shows_the_unavailable_message() {
        setScreen(CaptureStatus.Unavailable)

        composeRule.onNodeWithText("Scanning isn't available").assertIsDisplayed()
    }

    @Test
    fun test_that_add_receipt_manually_when_unavailable_calls_onAddManually() {
        setScreen(CaptureStatus.Unavailable)

        composeRule.onNodeWithText("Add receipt manually").performClick()

        assertThat(manualAdds).isEqualTo(1)
    }

    @Test
    fun test_that_read_failed_shows_the_failure_message() {
        setScreen(CaptureStatus.ReadFailed)

        composeRule.onNodeWithText("Couldn't read this receipt").assertIsDisplayed()
    }

    @Test
    fun test_that_try_again_when_read_failed_calls_onRetry() {
        setScreen(CaptureStatus.ReadFailed)

        composeRule.onNodeWithText("Try again").performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun test_that_add_manually_when_read_failed_calls_onAddManually() {
        setScreen(CaptureStatus.ReadFailed)

        composeRule.onNodeWithText("Add manually").performClick()

        assertThat(manualAdds).isEqualTo(1)
    }

    @Test
    fun test_that_scanning_shows_no_actions() {
        setScreen(CaptureStatus.Scanning)

        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }
}
