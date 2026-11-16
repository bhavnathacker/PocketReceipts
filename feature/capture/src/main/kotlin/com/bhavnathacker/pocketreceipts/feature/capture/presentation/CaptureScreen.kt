package com.bhavnathacker.pocketreceipts.feature.capture.presentation

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.capture.R
import com.bhavnathacker.pocketreceipts.feature.capture.presentation.model.CaptureStatus
import com.bhavnathacker.pocketreceipts.feature.capture.presentation.model.CaptureUiState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import de.palm.composestateevents.EventEffect
import timber.log.Timber

/**
 * Opens the document scanner once per attempt and hands its image to
 * [CaptureViewModel]. The scanner itself runs in Google Play services.
 */
@Composable
internal fun CaptureRoute(
    onScanned: (ReceiptScan) -> Unit,
    onAddManually: () -> Unit,
    onClose: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val imageUri = if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.firstOrNull()?.imageUri
        } else {
            null
        }
        if (imageUri != null) viewModel.onImageScanned(imageUri.toString()) else viewModel.onCancel()
    }
    // Survives rotation and recreation, so the scanner opens once per attempt.
    var launchedAttempt by rememberSaveable { mutableIntStateOf(NOT_LAUNCHED) }

    LaunchedEffect(uiState.status, uiState.scanAttempt) {
        if (uiState.status != CaptureStatus.Scanning || launchedAttempt == uiState.scanAttempt) {
            return@LaunchedEffect
        }
        launchedAttempt = uiState.scanAttempt
        if (activity == null) {
            viewModel.onScannerUnavailable()
            return@LaunchedEffect
        }
        runCatching { GmsDocumentScanning.getClient(scannerOptions).getStartScanIntent(activity) }
            .onSuccess { task ->
                task.addOnSuccessListener { scannerLauncher.launch(IntentSenderRequest.Builder(it).build()) }
                    .addOnFailureListener {
                        Timber.w(it, "Document scanner unavailable")
                        viewModel.onScannerUnavailable()
                    }
            }
            .onFailure {
                Timber.w(it, "Document scanner unavailable")
                viewModel.onScannerUnavailable()
            }
    }
    EventEffect(event = uiState.scannedEvent, onConsumed = viewModel::onScannedEventConsumed) { scan ->
        onScanned(scan)
    }
    EventEffect(event = uiState.closeEvent, onConsumed = viewModel::onCloseEventConsumed) {
        onClose()
    }

    CaptureScreen(
        uiState = uiState,
        onCancel = viewModel::onCancel,
        onRetry = viewModel::onRetry,
        onAddManually = onAddManually,
    )
}

private const val NOT_LAUNCHED = -1

private val scannerOptions = GmsDocumentScannerOptions.Builder()
    .setPageLimit(1)
    .setGalleryImportAllowed(true)
    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .build()

/**
 * What the app shows around the document scanner: nothing while it is open, then
 * reading progress, or why the scan can't continue.
 */
@Composable
internal fun CaptureScreen(
    uiState: CaptureUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onAddManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (uiState.status) {
                CaptureStatus.Scanning -> Unit
                CaptureStatus.Reading -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.capture_reading), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.capture_cancel)) }
                }
                CaptureStatus.Unavailable -> {
                    Message(
                        title = stringResource(R.string.capture_unavailable_title),
                        body = stringResource(R.string.capture_unavailable_body),
                    )
                    Button(onClick = onAddManually) { Text(stringResource(R.string.capture_add_receipt_manually)) }
                }
                CaptureStatus.ReadFailed -> {
                    Message(
                        title = stringResource(R.string.capture_read_failed_title),
                        body = stringResource(R.string.capture_read_failed_body),
                    )
                    Button(onClick = onRetry) { Text(stringResource(R.string.capture_try_again)) }
                    OutlinedButton(onClick = onAddManually) { Text(stringResource(R.string.capture_add_manually)) }
                }
            }
        }
    }
}

@Composable
private fun Message(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Text(text = body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
}

@PreviewLightDark
@Composable
private fun CaptureScreenReadingPreview() {
    PocketReceiptsTheme {
        CaptureScreen(CaptureUiState(status = CaptureStatus.Reading), onCancel = {}, onRetry = {}, onAddManually = {})
    }
}

@PreviewLightDark
@Composable
private fun CaptureScreenUnavailablePreview() {
    PocketReceiptsTheme {
        CaptureScreen(CaptureUiState(status = CaptureStatus.Unavailable), onCancel = {}, onRetry = {}, onAddManually = {})
    }
}

@PreviewLightDark
@Composable
private fun CaptureScreenReadFailedPreview() {
    PocketReceiptsTheme {
        CaptureScreen(CaptureUiState(status = CaptureStatus.ReadFailed), onCancel = {}, onRetry = {}, onAddManually = {})
    }
}
