package com.bhavnathacker.pocketreceipts.feature.settings.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bhavnathacker.pocketreceipts.core.ui.component.PlaceholderScreen
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.settings.R

/**
 * Placeholder until the Settings feature is built.
 */
@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_title),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PocketReceiptsTheme {
        Surface {
            SettingsScreen()
        }
    }
}
