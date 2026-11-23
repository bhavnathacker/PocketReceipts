# Proposal

## Why

Settings is an empty placeholder and nothing is planned for it. The owner wants only two tabs: Receipts and Ask.

## What Changes

- **BREAKING:** the Settings top-level destination is removed. Navigation shows Receipts and Ask, in that order.
- The `:feature:settings` module is deleted, along with its navigation key, screen and strings, and the app's `nav_settings` string.
- The Settings icons (`ic_settings`, `ic_settings_filled`) and `PlaceholderScreen`, which only Settings used, are deleted from `:core:ui`.
- The placeholder requirement is removed, because no destination is a placeholder any more.

## Capabilities

### New Capabilities

### Modified Capabilities
- `app-navigation`: there are now two top-level destinations, and the rotation scenario uses Ask. "Back navigation between top-level destinations" is replaced by "Back navigation to Receipts", with the same behaviour for Ask (its scenarios named Settings). "Placeholder destination content" is removed.
- `model-setup`: "App gated on the model" no longer lists Settings among the screens that setup blocks.

## Impact

- **`:app`:**
  - `TopLevelDestination` drops `SETTINGS`.
  - `PocketReceiptsApp` no longer registers `settingsEntries()`.
  - `build.gradle.kts` drops the dependency on `:feature:settings`.
  - The navigation tests (`PocketReceiptsAppTest`, `ModelSetupGateTest`, `TopLevelNavigatorTest`) are updated.
- **`settings.gradle.kts`:** no longer includes `:feature:settings`.
- **`:core:ui`:** `AppIcons` and `PlaceholderScreen` change, as above.
- **README:** the status line and the modules diagram are updated.
- **No data, permissions or other behaviour change.**
