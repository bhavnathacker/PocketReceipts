package com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.modelsetup.R
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupError
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupStatus
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupUiState

/**
 * Shows [content] once the on-device model is ready. Until then it shows only
 * the model setup screen, or just the background while setup is still finding
 * out whether the model is on the phone.
 */
@Composable
fun ModelSetupGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val viewModel: ModelSetupViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState.status) {
        ModelSetupStatus.Checking -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        ModelSetupStatus.Ready -> content()
        else -> ModelSetupScreen(
            uiState = uiState,
            onDownloadClick = viewModel::onDownloadClick,
            onRetryClick = viewModel::onRetryClick,
            modifier = modifier,
        )
    }
}

/**
 * The model setup screen: what the receipt reader is, its download size, the
 * download's progress, and any failure with Retry.
 */
@Composable
internal fun ModelSetupScreen(
    uiState: ModelSetupUiState,
    onDownloadClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.model_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.model_setup_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                SetupStatus(status = uiState.status, onDownloadClick = onDownloadClick, onRetryClick = onRetryClick)
            }
        }
    }
}

@Composable
private fun SetupStatus(
    status: ModelSetupStatus,
    onDownloadClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    when (status) {
        is ModelSetupStatus.NotDownloaded -> {
            Text(
                text = stringResource(R.string.model_setup_download_size, fileSize(status.sizeBytes)),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onDownloadClick) { Text(stringResource(R.string.model_setup_download)) }
        }
        is ModelSetupStatus.Downloading -> Progress(
            text = stringResource(R.string.model_setup_downloading, status.percent),
            progress = status.percent / 100f,
        )
        ModelSetupStatus.Verifying -> Progress(text = stringResource(R.string.model_setup_verifying))
        ModelSetupStatus.Preparing -> Progress(text = stringResource(R.string.model_setup_preparing))
        is ModelSetupStatus.Failed -> {
            Text(
                text = errorText(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Button(onClick = onRetryClick) { Text(stringResource(R.string.model_setup_retry)) }
        }
        ModelSetupStatus.Checking, ModelSetupStatus.Ready -> Unit
    }
}

@Composable
private fun Progress(text: String, progress: Float? = null) {
    if (progress == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun errorText(status: ModelSetupStatus.Failed): String = when (status.error) {
    ModelSetupError.NO_CONNECTION -> stringResource(R.string.model_setup_error_no_connection)
    ModelSetupError.NOT_ENOUGH_STORAGE ->
        stringResource(R.string.model_setup_error_not_enough_storage, fileSize(status.requiredBytes))
    ModelSetupError.DOWNLOAD_FAILED -> stringResource(R.string.model_setup_error_download_failed)
    ModelSetupError.CANNOT_RUN -> stringResource(R.string.model_setup_error_cannot_run)
}

@Composable
private fun fileSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

@PreviewLightDark
@Composable
private fun ModelSetupScreenNotDownloadedPreview() {
    PocketReceiptsTheme {
        ModelSetupScreen(
            uiState = ModelSetupUiState(ModelSetupStatus.NotDownloaded(2_588_147_712L)),
            onDownloadClick = {},
            onRetryClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ModelSetupScreenDownloadingPreview() {
    PocketReceiptsTheme {
        ModelSetupScreen(
            uiState = ModelSetupUiState(ModelSetupStatus.Downloading(42)),
            onDownloadClick = {},
            onRetryClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ModelSetupScreenFailedPreview() {
    PocketReceiptsTheme {
        ModelSetupScreen(
            uiState = ModelSetupUiState(ModelSetupStatus.Failed(ModelSetupError.NO_CONNECTION)),
            onDownloadClick = {},
            onRetryClick = {},
        )
    }
}
