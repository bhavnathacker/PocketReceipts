package com.bhavnathacker.pocketreceipts.feature.settings.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The Settings top-level destination.
 */
@Serializable
data object SettingsKey : NavKey

/**
 * Registers the Settings destinations with the app's [NavDisplay][androidx.navigation3.ui.NavDisplay].
 */
fun EntryProviderScope<NavKey>.settingsEntries() {
    entry<SettingsKey> {
        SettingsScreen()
    }
}
