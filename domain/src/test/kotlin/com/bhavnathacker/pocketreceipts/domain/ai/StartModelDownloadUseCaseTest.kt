package com.bhavnathacker.pocketreceipts.domain.ai

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartModelDownloadUseCaseTest {
    private lateinit var underTest: StartModelDownloadUseCase

    private val modelRepository = mock<ModelRepository>()

    @BeforeAll
    fun setUp() {
        underTest = StartModelDownloadUseCase(modelRepository = modelRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(modelRepository)
    }

    @Test
    fun `test that invoke starts the download`() = runTest {
        underTest()

        verify(modelRepository).startDownload()
    }
}
