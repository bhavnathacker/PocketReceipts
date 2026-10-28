package com.bhavnathacker.pocketreceipts.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.bhavnathacker.pocketreceipts.R
import com.bhavnathacker.pocketreceipts.core.ui.icon.AppIcons
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.AskKey
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ReceiptsKey
import com.bhavnathacker.pocketreceipts.feature.settings.presentation.SettingsKey

/**
 * Destinations shown in the app's navigation bar or rail, in display order.
 */
enum class TopLevelDestination(
    val key: NavKey,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
) {
    RECEIPTS(ReceiptsKey, R.string.nav_receipts, AppIcons.Receipts, AppIcons.ReceiptsFilled),
    ASK(AskKey, R.string.nav_ask, AppIcons.Ask, AppIcons.AskFilled),
    SETTINGS(SettingsKey, R.string.nav_settings, AppIcons.Settings, AppIcons.SettingsFilled),
}
