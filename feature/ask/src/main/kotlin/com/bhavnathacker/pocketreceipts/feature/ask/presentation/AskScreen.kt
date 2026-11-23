package com.bhavnathacker.pocketreceipts.feature.ask.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavnathacker.pocketreceipts.core.ui.R as CoreUiR
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.ask.R
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskAnswerUiModel
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskTotalUiModel
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskUiState

/** The example questions, in the order they are offered. */
private val Examples = listOf(
    R.string.ask_example_groceries_october,
    R.string.ask_example_total_2025,
    R.string.ask_example_shopping_quarter,
    R.string.ask_example_last_month,
)

@Composable
internal fun AskRoute(viewModel: AskViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Keep the model loaded only while Ask is on screen; leaving it, or the app going to the
    // background, ends the hold and the usual release rules apply.
    LifecycleStartEffect(viewModel) {
        viewModel.onShown()
        onStopOrDispose { viewModel.onHidden() }
    }
    AskScreen(
        uiState = uiState,
        onQuestionChange = viewModel::onQuestionChange,
        onExampleSelected = viewModel::onExampleSelected,
        onAsk = viewModel::onAsk,
    )
}

/**
 * The Ask destination: a spending question typed in plain language, the answer worked out on
 * the phone (with how the question was read), and example questions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AskScreen(
    uiState: AskUiState,
    onQuestionChange: (String) -> Unit,
    onExampleSelected: (String) -> Unit,
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ask_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.question,
                onValueChange = onQuestionChange,
                label = { Text(stringResource(R.string.ask_question_label)) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { onAsk() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onAsk,
                enabled = uiState.canAsk,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.ask_action))
            }
            AnswerContent(uiState)
            Examples(onExampleSelected)
        }
    }
}

@Composable
private fun AnswerContent(uiState: AskUiState) {
    val answer = uiState.answer
    when {
        uiState.isAnswering -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.ask_answering))
        }

        uiState.hasError -> Message(
            text = stringResource(R.string.ask_error),
            color = MaterialTheme.colorScheme.error,
        )

        answer is AskAnswerUiModel.Totals -> AnswerCard(answer.categoryLabel, answer.period) {
            answer.totals.forEach { TotalLine(it) }
        }

        answer is AskAnswerUiModel.NothingSpent -> AnswerCard(answer.categoryLabel, answer.period) {
            Text(stringResource(R.string.ask_nothing_spent), style = MaterialTheme.typography.bodyLarge)
        }

        answer == AskAnswerUiModel.Unanswerable -> Message(text = stringResource(R.string.ask_unanswerable))

        else -> Unit
    }
}

@Composable
private fun Message(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun AnswerCard(
    @StringRes categoryLabel: Int,
    period: String?,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(categoryLabel), style = MaterialTheme.typography.titleMedium)
            Text(
                text = period ?: stringResource(R.string.ask_all_time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun TotalLine(total: AskTotalUiModel) {
    Column {
        Text(total.amount, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = pluralStringResource(R.plurals.ask_receipt_count, total.receiptCount, total.receiptCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Examples(onExampleSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.ask_examples_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Examples.forEach { example ->
            val text = stringResource(example)
            SuggestionChip(
                onClick = { onExampleSelected(text) },
                label = { Text(text) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AskScreenEmptyPreview() {
    PocketReceiptsTheme {
        Surface {
            AskScreen(uiState = AskUiState(), onQuestionChange = {}, onExampleSelected = {}, onAsk = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun AskScreenAnswerPreview() {
    PocketReceiptsTheme {
        Surface {
            AskScreen(
                uiState = AskUiState(
                    question = "How much did I spend on groceries in October?",
                    answer = AskAnswerUiModel.Totals(
                        categoryLabel = CoreUiR.string.category_groceries,
                        period = "1 – 31 Oct 2026",
                        totals = listOf(AskTotalUiModel("₹1,500.00", 3), AskTotalUiModel("€12.30", 1)),
                    ),
                ),
                onQuestionChange = {},
                onExampleSelected = {},
                onAsk = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AskScreenAnsweringPreview() {
    PocketReceiptsTheme {
        Surface {
            AskScreen(
                uiState = AskUiState(question = "How much did I spend last month?", isAnswering = true),
                onQuestionChange = {},
                onExampleSelected = {},
                onAsk = {},
            )
        }
    }
}
