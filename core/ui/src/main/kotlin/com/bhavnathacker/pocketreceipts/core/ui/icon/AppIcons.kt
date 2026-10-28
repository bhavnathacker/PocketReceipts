package com.bhavnathacker.pocketreceipts.core.ui.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.bhavnathacker.pocketreceipts.core.ui.R
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme

/**
 * Material Symbols used across the app. Outlined icons are the default; the
 * `Filled` variants mark a selected navigation destination.
 */
object AppIcons {
    @DrawableRes val Receipts = R.drawable.ic_receipt_long
    @DrawableRes val ReceiptsFilled = R.drawable.ic_receipt_long_filled
    @DrawableRes val Ask = R.drawable.ic_chat
    @DrawableRes val AskFilled = R.drawable.ic_chat_filled
    @DrawableRes val Settings = R.drawable.ic_settings
    @DrawableRes val SettingsFilled = R.drawable.ic_settings_filled
}

@PreviewLightDark
@Composable
private fun AppIconsPreview() {
    PocketReceiptsTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                listOf(
                    AppIcons.Receipts,
                    AppIcons.ReceiptsFilled,
                    AppIcons.Ask,
                    AppIcons.AskFilled,
                    AppIcons.Settings,
                    AppIcons.SettingsFilled,
                ).forEach { icon ->
                    Icon(painter = painterResource(icon), contentDescription = null)
                }
            }
        }
    }
}
