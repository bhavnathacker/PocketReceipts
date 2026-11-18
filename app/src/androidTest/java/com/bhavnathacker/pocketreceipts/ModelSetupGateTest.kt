package com.bhavnathacker.pocketreceipts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.ModelSetupGate
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * The app shell behind model setup, as `MainActivity` shows it, with a fake model
 * repository standing in for the real download.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ModelSetupGateTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var modelRepository: FakeModelRepository

    private val compact = WindowSizeClass(minWidthDp = 0, minHeightDp = 0)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun setGate(state: ModelState) {
        modelRepository.state.value = state
        composeRule.setContent {
            PocketReceiptsTheme {
                ModelSetupGate {
                    PocketReceiptsApp(windowSizeClass = compact)
                }
            }
        }
    }

    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun test_that_first_launch_shows_setup_without_navigation() {
        setGate(ModelState.NotDownloaded(2_588_147_712L))

        composeRule.onNodeWithText("Set up the receipt reader").assertIsDisplayed()
        composeRule.onNodeWithText("Receipts").assertDoesNotExist()
        composeRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun test_that_tapping_download_starts_the_download() {
        setGate(ModelState.NotDownloaded(2_588_147_712L))

        composeRule.onNodeWithText("Download").performClick()
        composeRule.waitForIdle()

        assertThat(modelRepository.downloadsStarted).isEqualTo(1)
    }

    @Test
    fun test_that_a_ready_model_opens_receipts() {
        setGate(ModelState.Ready)

        composeRule.onNodeWithText("Set up the receipt reader").assertDoesNotExist()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun test_that_back_from_setup_closes_the_app() {
        setGate(ModelState.NotDownloaded(2_588_147_712L))

        pressBack()

        assertThat(composeRule.activity.isFinishing).isTrue()
    }

    @Test
    fun test_that_finishing_setup_shows_receipts() {
        setGate(ModelState.Downloading(downloadedBytes = 1, totalBytes = 2))

        modelRepository.state.value = ModelState.Ready
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Set up the receipt reader").assertDoesNotExist()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun test_that_back_from_receipts_after_setup_closes_the_app() {
        setGate(ModelState.Downloading(downloadedBytes = 1, totalBytes = 2))
        modelRepository.state.value = ModelState.Ready
        composeRule.waitForIdle()

        pressBack()

        assertThat(composeRule.activity.isFinishing).isTrue()
    }
}
