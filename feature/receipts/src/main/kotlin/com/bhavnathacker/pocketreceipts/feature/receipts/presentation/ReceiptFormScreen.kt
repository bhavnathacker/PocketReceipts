package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavnathacker.pocketreceipts.core.ui.icon.AppIcons
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ReceiptCategoryLabelMapper
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptFormUiState
import de.palm.composestateevents.EventEffect
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency

@Composable
internal fun ReceiptFormRoute(
    receiptId: Long?,
    scan: ScanPrefill?,
    onClose: () -> Unit,
    viewModel: ReceiptFormViewModel = hiltViewModel<ReceiptFormViewModel, ReceiptFormViewModel.Factory>(
        creationCallback = { factory -> factory.create(receiptId, scan) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.receipt_form_save_failed)

    EventEffect(event = uiState.closeEvent, onConsumed = viewModel::onCloseEventConsumed) {
        onClose()
    }
    EventEffect(event = uiState.saveFailedEvent, onConsumed = viewModel::onSaveFailedEventConsumed) {
        snackbarHostState.showSnackbar(saveFailedMessage)
    }

    ReceiptFormScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onClose = onClose,
        onSave = viewModel::onSave,
        onMerchantChange = viewModel::onMerchantChange,
        onAmountChange = viewModel::onAmountChange,
        onCurrencyChange = viewModel::onCurrencyChange,
        onDateChange = viewModel::onDateChange,
        onCategoryChange = viewModel::onCategoryChange,
        onNoteChange = viewModel::onNoteChange,
        onNothingRecognizedEventConsumed = viewModel::onNothingRecognizedEventConsumed,
    )
}

/**
 * The form for adding a receipt by hand, editing one, or reviewing a scanned
 * one prefilled with the values read from it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceiptFormScreen(
    uiState: ReceiptFormUiState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onCategoryChange: (ReceiptCategory) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNothingRecognizedEventConsumed: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val nothingRecognizedMessage = stringResource(R.string.receipt_form_nothing_recognized)

    EventEffect(event = uiState.nothingRecognizedEvent, onConsumed = onNothingRecognizedEventConsumed) {
        snackbarHostState.showSnackbar(nothingRecognizedMessage)
    }
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(AppIcons.Close),
                            contentDescription = stringResource(R.string.receipt_form_close),
                        )
                    }
                },
                title = {
                    Text(
                        stringResource(
                            when {
                                uiState.isEditing -> R.string.receipt_form_edit_title
                                uiState.isReview -> R.string.receipt_form_review_title
                                else -> R.string.receipt_form_add_title
                            },
                        ),
                    )
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isSaving && !uiState.isLoading) {
                        Text(stringResource(R.string.receipt_form_save))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = onMerchantChange,
                label = { Text(stringResource(R.string.receipt_form_merchant)) },
                singleLine = true,
                isError = uiState.merchantError != null,
                supportingText = uiState.merchantError?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.receipt_form_amount)) },
                    singleLine = true,
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(stringResource(it)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                ReadOnlyField(
                    value = uiState.currencyCode,
                    label = stringResource(R.string.receipt_form_currency),
                    onClick = { showCurrencyPicker = true },
                    modifier = Modifier.width(120.dp),
                )
            }
            ReadOnlyField(
                value = rememberDateText(uiState.purchaseDate),
                label = stringResource(R.string.receipt_form_date),
                onClick = { showDatePicker = true },
                errorText = uiState.dateError?.let { stringResource(it) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(AppIcons.Calendar),
                            contentDescription = stringResource(R.string.receipt_form_choose_date),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            CategoryField(category = uiState.category, onCategoryChange = onCategoryChange)
            OutlinedTextField(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.receipt_form_note)) },
                minLines = 3,
                isError = uiState.noteError != null,
                supportingText = uiState.noteError?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        PurchaseDatePickerDialog(
            date = uiState.purchaseDate,
            today = uiState.today,
            onDateSelected = {
                onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            onCurrencySelected = {
                onCurrencyChange(it)
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false },
        )
    }
}

/**
 * A text field that can't be typed into; tapping it runs [onClick].
 */
@Composable
private fun ReadOnlyField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it) } },
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
        )
        // Covers the field (but not a trailing button, which handles its own clicks) to open the picker.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (trailingIcon != null) 48.dp else 0.dp)
                .clickable(onClickLabel = label, onClick = onClick),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    category: ReceiptCategory,
    onCategoryChange: (ReceiptCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(category.labelRes()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.receipt_form_category)) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReceiptCategory.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes())) },
                    onClick = {
                        onCategoryChange(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDatePickerDialog(
    date: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = today.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onDateSelected(it.toUtcLocalDate()) } ?: onDismiss()
                },
            ) {
                Text(stringResource(R.string.receipt_form_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.receipt_form_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun CurrencyPickerDialog(
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val currencies = remember(locale) {
        Currency.getAvailableCurrencies()
            .filter { it.defaultFractionDigits >= 0 }
            .sortedBy { it.currencyCode }
            .map { it.currencyCode to it.getDisplayName(locale) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, currencies) {
        currencies.filter { (code, name) ->
            code.contains(query.trim(), ignoreCase = true) || name.contains(query.trim(), ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.receipt_form_currency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.receipt_form_currency_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.first }) { (code, name) ->
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.receipt_form_currency_option, code, name)) },
                            modifier = Modifier.clickable { onCurrencySelected(code) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.receipt_form_cancel)) }
        },
    )
}

@Composable
private fun rememberDateText(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }
}

/** Material's date picker works in UTC milliseconds at midnight. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val categoryLabelMapper = ReceiptCategoryLabelMapper()

@StringRes
private fun ReceiptCategory.labelRes(): Int = categoryLabelMapper(this)

@PreviewLightDark
@Composable
private fun ReceiptFormScreenReviewPreview() {
    PocketReceiptsTheme {
        ReceiptFormScreen(
            uiState = ReceiptFormUiState(
                isEditing = false,
                isReview = true,
                isLoading = false,
                today = LocalDate.of(2026, 3, 3),
                merchant = "Corner Shop",
                amount = "210.00",
                currencyCode = "INR",
                purchaseDate = LocalDate.of(2026, 3, 2),
            ),
            onClose = {},
            onSave = {},
            onMerchantChange = {},
            onAmountChange = {},
            onCurrencyChange = {},
            onDateChange = {},
            onCategoryChange = {},
            onNoteChange = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ReceiptFormScreenPreview() {
    PocketReceiptsTheme {
        ReceiptFormScreen(
            uiState = ReceiptFormUiState(
                isEditing = false,
                isLoading = false,
                today = LocalDate.of(2026, 3, 3),
                merchant = "",
                amount = "4.50",
                currencyCode = "EUR",
                purchaseDate = LocalDate.of(2026, 3, 3),
                merchantError = R.string.receipt_form_error_merchant_blank,
            ),
            onClose = {},
            onSave = {},
            onMerchantChange = {},
            onAmountChange = {},
            onCurrencyChange = {},
            onDateChange = {},
            onCategoryChange = {},
            onNoteChange = {},
        )
    }
}
