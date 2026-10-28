package com.bhavnathacker.pocketreceipts.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Switches between top-level destinations on a single back stack whose first
 * entry is always [startKey]:
 * - selecting [startKey] leaves `[start]`
 * - selecting another destination X leaves `[start, X]`
 * - reselecting the selected destination changes nothing
 *
 * So back from any other destination returns to [startKey], and back from
 * [startKey] leaves the app.
 */
class TopLevelNavigator(
    private val backStack: MutableList<NavKey>,
    private val startKey: NavKey,
    private val topLevelKeys: Set<NavKey>,
) {

    /** The top-level destination nearest the top of the back stack. */
    val selectedKey: NavKey
        get() = backStack.lastOrNull { it in topLevelKeys } ?: startKey

    fun select(key: NavKey) {
        if (key == selectedKey) return
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        if (key != startKey) {
            backStack.add(key)
        }
    }
}
