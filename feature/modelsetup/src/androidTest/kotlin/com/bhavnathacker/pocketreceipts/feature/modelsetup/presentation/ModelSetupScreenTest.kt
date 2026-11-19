package com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation

import android.text.format.Formatter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupError
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupStatus
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelSetupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var downloadClicks = 0
    private var retryClicks = 0

    private fun setScreen(status: ModelSetupStatus) {
        composeRule.setContent {
            PocketReceiptsTheme {
                ModelSetupScreen(
                    uiState = ModelSetupUiState(status),
                    onDownloadClick = { downloadClicks++ },
                    onRetryClick = { retryClicks++ },
                )
            }
        }
    }

    @Test
    fun test_that_setup_offers_the_download_with_its_size() {
        setScreen(ModelSetupStatus.NotDownloaded(2_588_147_712L))

        composeRule.onNodeWithText("The receipt reader is a one-time download of 2.6 GB.").assertIsDisplayed()
        composeRule.onNodeWithText("Download").assertIsDisplayed()
    }

    @Test
    fun test_that_tapping_download_reports_it() {
        setScreen(ModelSetupStatus.NotDownloaded(2_588_147_712L))

        composeRule.onNodeWithText("Download").performClick()

        assertThat(downloadClicks).isEqualTo(1)
    }

    @Test
    fun test_that_setup_shows_the_download_progress() {
        setScreen(ModelSetupStatus.Downloading(42))

        composeRule.onNodeWithText("Downloading… 42%").assertIsDisplayed()
        composeRule.onNodeWithText("Download").assertDoesNotExist()
    }

    @Test
    fun test_that_setup_shows_that_the_download_is_being_checked() {
        setScreen(ModelSetupStatus.Verifying)

        composeRule.onNodeWithText("Checking the download…").assertIsDisplayed()
    }

    @Test
    fun test_that_setup_shows_that_the_reader_is_being_prepared() {
        setScreen(ModelSetupStatus.Preparing)

        composeRule.onNodeWithText("Getting the receipt reader ready…").assertIsDisplayed()
    }

    @Test
    fun test_that_setup_explains_a_missing_connection_and_offers_retry() {
        setScreen(ModelSetupStatus.Failed(ModelSetupError.NO_CONNECTION))

        composeRule.onNodeWithText("No internet connection", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertThat(retryClicks).isEqualTo(1)
    }

    @Test
    fun test_that_setup_names_the_storage_needed() {
        val required = 2_693_005_312L
        val size = Formatter.formatShortFileSize(InstrumentationRegistry.getInstrumentation().targetContext, required)
        setScreen(ModelSetupStatus.Failed(ModelSetupError.NOT_ENOUGH_STORAGE, requiredBytes = required))

        composeRule.onNodeWithText("Not enough storage. The receipt reader needs $size of free space.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertThat(retryClicks).isEqualTo(1)
    }

    @Test
    fun test_that_setup_reports_a_failed_download_and_offers_retry() {
        setScreen(ModelSetupStatus.Failed(ModelSetupError.DOWNLOAD_FAILED))

        composeRule.onNodeWithText("The download failed.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertThat(retryClicks).isEqualTo(1)
    }

    @Test
    fun test_that_setup_says_when_the_phone_cannot_run_the_reader() {
        setScreen(ModelSetupStatus.Failed(ModelSetupError.CANNOT_RUN))

        composeRule.onNodeWithText("This phone can't run the receipt reader.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertThat(retryClicks).isEqualTo(1)
    }
}
