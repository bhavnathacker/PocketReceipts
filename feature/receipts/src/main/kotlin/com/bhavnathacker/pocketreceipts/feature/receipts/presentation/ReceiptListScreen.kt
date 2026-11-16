package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavnathacker.pocketreceipts.core.ui.icon.AppIcons
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListItemUiModel
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListUiState

@Composable
internal fun ReceiptListRoute(
    onReceiptClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: ReceiptListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReceiptListScreen(
        uiState = uiState,
        onReceiptClick = onReceiptClick,
        onAddClick = onAddClick,
    )
}

/**
 * The Receipts destination: the user's receipts, newest first, or an empty
 * state, with an action to add a receipt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceiptListScreen(
    uiState: ReceiptListUiState,
    onReceiptClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.receipts_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
            )
        },
        floatingActionButton = {
            // The content overload keeps the label in the semantics tree; the animated
            // text/icon overload hides it from accessibility services.
            ExtendedFloatingActionButton(onClick = onAddClick) {
                Icon(painter = painterResource(AppIcons.Add), contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.receipt_add))
            }
        },
    ) { padding ->
        when (uiState) {
            ReceiptListUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            ReceiptListUiState.Empty -> CenteredMessage(
                title = stringResource(R.string.receipt_list_empty_title),
                body = stringResource(R.string.receipt_list_empty_body),
                contentPadding = padding,
            )

            ReceiptListUiState.Error -> CenteredMessage(
                title = stringResource(R.string.receipt_list_error),
                body = null,
                contentPadding = padding,
            )

            is ReceiptListUiState.Receipts -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    // Keep the last row clear of the floating action button.
                    bottom = padding.calculateBottomPadding() + FabClearance,
                ),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    ReceiptRow(item = item, onClick = { onReceiptClick(item.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    item: ReceiptListItemUiModel,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(item.merchant) },
        supportingContent = {
            Text(stringResource(R.string.receipt_list_item_supporting, item.date, stringResource(item.categoryLabel)))
        },
        trailingContent = {
            Text(text = item.total, style = MaterialTheme.typography.titleMedium)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun CenteredMessage(
    title: String,
    body: String?,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) {
            Text(text = body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

private val FabClearance = 88.dp

@PreviewLightDark
@Composable
private fun ReceiptListScreenPreview() {
    PocketReceiptsTheme {
        ReceiptListScreen(
            uiState = ReceiptListUiState.Receipts(
                listOf(
                    ReceiptListItemUiModel(1, "Corner Shop", "3 Mar 2026", "₹1,23,456.50", R.string.category_groceries),
                    ReceiptListItemUiModel(2, "Ramen Ya", "2 Mar 2026", "¥1,500", R.string.category_dining),
                ),
            ),
            onReceiptClick = {},
            onAddClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ReceiptListScreenEmptyPreview() {
    PocketReceiptsTheme {
        ReceiptListScreen(uiState = ReceiptListUiState.Empty, onReceiptClick = {}, onAddClick = {})
    }
}
