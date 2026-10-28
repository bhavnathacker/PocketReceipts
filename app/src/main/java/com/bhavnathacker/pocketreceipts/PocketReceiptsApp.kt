package com.bhavnathacker.pocketreceipts

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.askEntries
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ReceiptsKey
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.receiptsEntries
import com.bhavnathacker.pocketreceipts.feature.settings.presentation.settingsEntries
import com.bhavnathacker.pocketreceipts.navigation.TopLevelDestination
import com.bhavnathacker.pocketreceipts.navigation.TopLevelNavigator

/**
 * The app shell: top-level navigation (bar or rail, depending on
 * [windowSizeClass]) around a [NavDisplay] showing the current destination.
 */
@Composable
fun PocketReceiptsApp(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val backStack = rememberNavBackStack(ReceiptsKey)
    val navigator = remember(backStack) {
        TopLevelNavigator(
            backStack = backStack,
            startKey = ReceiptsKey,
            topLevelKeys = TopLevelDestination.entries.map { it.key }.toSet(),
        )
    }
    val selectedKey = navigator.selectedKey

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination.key == selectedKey
                item(
                    selected = selected,
                    onClick = { navigator.select(destination.key) },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) destination.selectedIcon else destination.icon),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
        modifier = modifier,
        layoutType = navigationSuiteType(windowSizeClass),
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                receiptsEntries()
                askEntries()
                settingsEntries()
            },
        )
    }
}

/**
 * Bottom navigation bar for compact widths, navigation rail from medium width up.
 */
internal fun navigationSuiteType(windowSizeClass: WindowSizeClass): NavigationSuiteType =
    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }
