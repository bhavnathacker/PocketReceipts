package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The Receipts top-level destination.
 */
@Serializable
data object ReceiptsKey : NavKey

/**
 * Registers the Receipts destinations with the app's [NavDisplay][androidx.navigation3.ui.NavDisplay].
 */
fun EntryProviderScope<NavKey>.receiptsEntries() {
    entry<ReceiptsKey> {
        ReceiptsScreen()
    }
}
