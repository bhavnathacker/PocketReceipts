package com.bhavnathacker.pocketreceipts.feature.ask.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The Ask top-level destination.
 */
@Serializable
data object AskKey : NavKey

/**
 * Registers the Ask destinations with the app's [NavDisplay][androidx.navigation3.ui.NavDisplay].
 */
fun EntryProviderScope<NavKey>.askEntries() {
    entry<AskKey> {
        AskScreen()
    }
}
