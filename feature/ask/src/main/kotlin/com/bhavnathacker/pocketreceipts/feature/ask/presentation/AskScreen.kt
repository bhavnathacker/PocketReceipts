package com.bhavnathacker.pocketreceipts.feature.ask.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bhavnathacker.pocketreceipts.core.ui.component.PlaceholderScreen
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.ask.R

/**
 * Placeholder until the Ask feature is built.
 */
@Composable
internal fun AskScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.ask_title),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AskScreenPreview() {
    PocketReceiptsTheme {
        Surface {
            AskScreen()
        }
    }
}
