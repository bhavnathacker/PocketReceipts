package com.bhavnathacker.pocketreceipts.domain.ai

import javax.inject.Inject

/** Starts the model download, or starts it again after a failure. */
class StartModelDownloadUseCase @Inject constructor(
    private val modelRepository: ModelRepository,
) {
    suspend operator fun invoke() = modelRepository.startDownload()
}
