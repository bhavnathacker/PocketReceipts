# Tasks

## 1. Remove Settings

- [x] 1.1 Delete `:feature:settings`:
  - Remove it from `settings.gradle.kts` and `app/build.gradle.kts`.
  - Drop `TopLevelDestination.SETTINGS`, the `settingsEntries()` call and the `nav_settings` string.

  Verify that `./gradlew :app:assembleDebug` succeeds and that no `feature.settings` reference remains.
- [x] 1.2 Delete the Settings icons and `PlaceholderScreen` from `:core:ui`, along with the icons' preview entries. Verify that `./gradlew :core:ui:assembleDebug` succeeds and that neither is referenced anywhere.

## 2. Tests, docs and checks

- [x] 2.1 Update the app tests:
  - In `TopLevelNavigatorTest`, the fake `Settings` key is renamed `Other`. It is a generic third key that tests switching between two non-start destinations, which the navigator still supports.
  - In `PocketReceiptsAppTest`, the order test covers two destinations, a test checks there is no Settings tab, and the back and rotation tests use Ask.
  - `ModelSetupGateTest` no longer expects Settings.

  Verify that `./gradlew :app:testDebugUnitTest` passes, and that the `:app` connected tests pass on the emulator.
- [x] 2.2 Update the README (the status line and the modules diagram). Verify that `./gradlew check` and `openspec validate remove-settings-tab --strict` pass.

## Workflow follow-up

- Open a PR from `feature/remove-settings-tab` into `main`, and merge only after the owner reviews and approves.
- Archive the change with `/opsx:archive` after it is merged.
