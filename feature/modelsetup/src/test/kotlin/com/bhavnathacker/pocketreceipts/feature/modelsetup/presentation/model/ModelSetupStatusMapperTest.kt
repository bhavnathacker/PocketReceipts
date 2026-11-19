package com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model

import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelSetupStatusMapperTest {
    private lateinit var underTest: ModelSetupStatusMapper

    @BeforeEach
    fun setUp() {
        underTest = ModelSetupStatusMapper()
    }

    @Test
    fun `test that invoke rounds the download percentage down`() {
        assertThat(underTest(ModelState.Downloading(downloadedBytes = 999, totalBytes = 1000)))
            .isEqualTo(ModelSetupStatus.Downloading(99))
    }

    @Test
    fun `test that invoke shows zero percent when the total is unknown`() {
        assertThat(underTest(ModelState.Downloading(downloadedBytes = 10, totalBytes = 0)))
            .isEqualTo(ModelSetupStatus.Downloading(0))
    }

    @Test
    fun `test that invoke shows a finished download as one hundred percent`() {
        assertThat(underTest(ModelState.Downloading(downloadedBytes = 1000, totalBytes = 1000)))
            .isEqualTo(ModelSetupStatus.Downloading(100))
    }
}
