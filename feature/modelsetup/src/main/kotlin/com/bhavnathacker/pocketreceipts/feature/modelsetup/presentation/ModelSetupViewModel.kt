package com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import com.bhavnathacker.pocketreceipts.domain.ai.MonitorModelStateUseCase
import com.bhavnathacker.pocketreceipts.domain.ai.StartModelDownloadUseCase
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupError
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupStatus
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupStatusMapper
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Follows the on-device model's setup until it is ready, and starts its download.
 * Retry after "can't run" loads the model again; after any other failure it
 * downloads again.
 */
@HiltViewModel
internal class ModelSetupViewModel @Inject constructor(
    private val monitorModelStateUseCase: MonitorModelStateUseCase,
    private val startModelDownloadUseCase: StartModelDownloadUseCase,
    private val modelSetupStatusMapper: ModelSetupStatusMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSetupUiState())
    val uiState: StateFlow<ModelSetupUiState> = _uiState.asStateFlow()

    private var monitorJob: Job? = null

    init {
        monitorModelState()
    }

    private fun monitorModelState() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            monitorModelStateUseCase()
                .catch {
                    Timber.e(it, "Failed to follow the model setup")
                    _uiState.update { state -> state.copy(status = ModelSetupStatus.Failed(ModelSetupError.CANNOT_RUN)) }
                }
                .transformWhile { modelState ->
                    emit(modelState)
                    modelState != ModelState.Ready
                }
                .collect { modelState ->
                    _uiState.update { it.copy(status = modelSetupStatusMapper(modelState)) }
                }
        }
    }

    fun onDownloadClick() {
        startDownload()
    }

    fun onRetryClick() {
        val status = _uiState.value.status
        if (status is ModelSetupStatus.Failed && status.error == ModelSetupError.CANNOT_RUN) {
            _uiState.update { it.copy(status = ModelSetupStatus.Preparing) }
            monitorModelState()
        } else {
            startDownload()
        }
    }

    private fun startDownload() {
        viewModelScope.launch {
            runCatching {
                startModelDownloadUseCase()
            }.onFailure {
                Timber.e(it, "Failed to start the model download")
                _uiState.update { state ->
                    state.copy(status = ModelSetupStatus.Failed(ModelSetupError.DOWNLOAD_FAILED))
                }
            }
        }
    }
}
