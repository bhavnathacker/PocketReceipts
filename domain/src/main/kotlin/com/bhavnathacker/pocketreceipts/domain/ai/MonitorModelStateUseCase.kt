package com.bhavnathacker.pocketreceipts.domain.ai

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

/**
 * The model's setup state. When the downloaded model is verified but hasn't been
 * shown to load yet ([ModelState.Loading]), it is loaded once: on success the
 * state becomes [ModelState.Ready], otherwise [ModelState.CannotRun]. Collecting
 * again after [ModelState.CannotRun] retries the load.
 */
class MonitorModelStateUseCase @Inject constructor(
    private val modelRepository: ModelRepository,
    private val modelRuntimeRepository: ModelRuntimeRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ModelState> =
        modelRepository.monitorModelState().transformLatest { state ->
            emit(state)
            if (state == ModelState.Loading) {
                val path = modelRepository.modelPath()
                if (path != null && modelRuntimeRepository.canLoad(path)) {
                    modelRepository.markLoadVerified()
                } else {
                    emit(ModelState.CannotRun)
                }
            }
        }
}
