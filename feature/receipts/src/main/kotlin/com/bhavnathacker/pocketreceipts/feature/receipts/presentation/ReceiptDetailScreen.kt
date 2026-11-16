package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavnathacker.pocketreceipts.core.ui.icon.AppIcons
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiModel
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiState
import de.palm.composestateevents.EventEffect

@Composable
internal fun ReceiptDetailRoute(
    receiptId: Long,
    onEdit: () -> Unit,
    onClose: () -> Unit,
    viewModel: ReceiptDetailViewModel = hiltViewModel<ReceiptDetailViewModel, ReceiptDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(receiptId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.receipt_detail_delete_failed)

    EventEffect(event = uiState.closeEvent, onConsumed = viewModel::onCloseEventConsumed) {
        onClose()
    }
    EventEffect(event = uiState.deleteFailedEvent, onConsumed = viewModel::onDeleteFailedEventConsumed) {
        snackbarHostState.showSnackbar(deleteFailedMessage)
    }

    ReceiptDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onClose,
        onEdit = onEdit,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
    )
}

/**
 * One receipt's fields, with actions to edit or delete it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceiptDetailScreen(
    uiState: ReceiptDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(AppIcons.Back),
                            contentDescription = stringResource(R.string.receipt_detail_back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.receipt_detail_title)) },
                actions = {
                    if (uiState.receipt != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                painter = painterResource(AppIcons.Edit),
                                contentDescription = stringResource(R.string.receipt_detail_edit),
                            )
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                painter = painterResource(AppIcons.Delete),
                                contentDescription = stringResource(R.string.receipt_detail_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val receipt = uiState.receipt
        if (receipt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isLoading) CircularProgressIndicator()
            }
        } else {
            ReceiptDetailContent(
                receipt = receipt,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text(stringResource(R.string.receipt_detail_delete_title)) },
            text = { Text(stringResource(R.string.receipt_detail_delete_body)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text(stringResource(R.string.receipt_detail_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) {
                    Text(stringResource(R.string.receipt_detail_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun ReceiptDetailContent(
    receipt: ReceiptDetailUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = receipt.merchant,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = receipt.total, style = MaterialTheme.typography.displaySmall)
        }
        HorizontalDivider()
        DetailField(label = stringResource(R.string.receipt_detail_date), value = receipt.date)
        DetailField(label = stringResource(R.string.receipt_detail_category), value = stringResource(receipt.categoryLabel))
        if (receipt.note != null) {
            DetailField(label = stringResource(R.string.receipt_detail_note), value = receipt.note)
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@PreviewLightDark
@Composable
private fun ReceiptDetailScreenPreview() {
    PocketReceiptsTheme {
        ReceiptDetailScreen(
            uiState = ReceiptDetailUiState(
                isLoading = false,
                receipt = ReceiptDetailUiModel(
                    merchant = "Corner Shop",
                    date = "3 Mar 2026",
                    total = "₹1,23,456.50",
                    categoryLabel = R.string.category_groceries,
                    note = "Milk, bread and eggs",
                ),
            ),
            onBack = {},
            onEdit = {},
            onDeleteClick = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
        )
    }
}
