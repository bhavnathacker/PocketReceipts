# Tasks

## 1. Dependencies and Room convention plugin

- [x] 1.1 Add catalog entries for Room (runtime, ktx if still separate, compiler, testing, Gradle plugin), and the instrumented-test libraries `:data:receipts` needs (`androidx.test.ext:junit`, `androidx.test:runner`), choosing the newest stable versions compatible with AGP 9.3, Kotlin 2.2 and KSP 2.3. Verify with `./gradlew help` that the catalog resolves.
- [x] 1.2 Implement the `pocketreceipts.android.room` convention plugin (D3: Room Gradle plugin, KSP, `schemaDirectory("$projectDir/schemas")`, runtime + compiler dependencies), register it in `convention.gradle.kts`, and apply it to `:data:receipts`. Verify `./gradlew :data:receipts:assembleDebug` passes, runs twice with the configuration cache reused, and `./gradlew check` (privacy + module-dependency checks) still passes.

## 2. Domain model and use cases (`:domain`)

- [x] 2.1 Add `Money`, `ReceiptCategory` (nine values in spec order), `Receipt`, `ReceiptInput` and the `ReceiptRepository` interface (D1), with KDoc. Verify `./gradlew :domain:build` passes.
- [x] 2.2 TDD `ReceiptValidator`: blank merchant, merchant over 100 characters after trimming, total ≤ 0, future date (against a fixed `Clock`), note over 500 characters, and several errors at once. Verify `./gradlew :domain:test` passes.
- [x] 2.3 TDD (usecase skill) `MonitorReceiptsUseCase`, `MonitorReceiptUseCase`, `DeleteReceiptUseCase`, `GetDefaultCurrencyUseCase` (en-GB → GBP, de-DE → EUR, a locale with no region → INR) and `SaveReceiptUseCase`: it trims merchant and note, maps a blank note to null, returns `Invalid` without calling the repository, and adds when `id` is null or updates otherwise. Verify `./gradlew :domain:test` passes.

## 3. Storage and repository (`:data:receipts`)

- [x] 3.1 TDD (mapper skill) `ReceiptCategoryMapper` (stable keys both ways, unknown key → `OTHER`) and `ReceiptEntityMapper` (epoch-day dates, minor units, nullable note). Verify `./gradlew :data:receipts:testDebugUnitTest` passes.
- [x] 3.2 Add `ReceiptEntity`, `ReceiptDao` (`observeAll` ordered by `purchase_date DESC, created_at DESC`, `observeById`, `insert`, `update`, `deleteById`) and `ReceiptsDatabase` v1 with the `(purchase_date, created_at)` index (D4). Write instrumented DAO tests on an in-memory database: ordering with same-date ties, update, delete, and `observeById` emitting `null` after delete. Verify `./gradlew :data:receipts:connectedDebugAndroidTest` passes and `data/receipts/schemas/.../1.json` is generated and committed.
- [x] 3.3 Provide `ReceiptsDatabase` (plain Room, no encryption; D5) as a Hilt singleton and `ReceiptDao` from it. Verify `./gradlew :app:assembleDebug` passes (the Hilt graph compiles).
- [x] 3.4 TDD `DefaultReceiptRepository` with a mocked DAO and a fixed `Clock`: it maps rows, sets `created_at`/`updated_at` on add and only `updated_at` on update. Add the `internal` `ReceiptsDataModule` bindings (D7) and `TimeModule` (`Clock.systemDefaultZone()`) in `:app`. Verify `./gradlew :data:receipts:testDebugUnitTest :app:assembleDebug` passes (the Hilt graph compiles).

## 4. Backup exclusion (`:app`)

- [x] 4.1 Exclude the `database` domain in `backup_rules.xml` and in both `<cloud-backup>` and `<device-transfer>` of `data_extraction_rules.xml` (D6), replacing the template comments. Verify `./gradlew :app:lint` reports no backup-rule issues. Then on an emulator: add a receipt, run `adb shell bmgr backupnow com.bhavnathacker.pocketreceipts` with the local transport, and confirm the backup has no `databases/` entry. Record the result in the change notes.

## 5. Receipts presentation (`:feature:receipts`)

- [x] 5.1 Add the add, edit, delete, back and close Material Symbols drawables to `:core:ui` `AppIcons`. Verify each shows correctly in a `@Preview`.
- [x] 5.2 TDD (mapper skill) `AmountInputParser` (en-US `4.50`, de-DE `4,50`, en-IN `123456.50` and `1,23,456.50` both → `12345650` while `99.505` is rejected, JPY rejects decimals, too many decimals, a grouping separator after the decimal separator, non-numeric, empty) and `MoneyFormatter` / `ReceiptListItemUiMapper` (category label resource, formatted date and total). Verify `./gradlew :feature:receipts:testDebugUnitTest` passes, and the `MoneyFormatter` device tests (ICU, including en-IN `₹1,23,456.50`) pass with `:feature:receipts:connectedDebugAndroidTest`.
- [x] 5.3 Add `ReceiptDetailKey` and `ReceiptFormKey` (D8), and change `receiptsEntries(onNavigate, onBack)`. Wire it in `PocketReceiptsApp`, and add `TopLevelNavigator` unit tests for nested keys (Receipts stays selected with `[Receipts, Detail(1)]`; `select(Ask)` from `[Receipts, Detail(1), Form(1)]` gives `[Receipts, Ask]`). Verify `./gradlew :app:testDebugUnitTest assembleDebug` passes.
- [x] 5.4 TDD (viewmodel skill) `ReceiptListViewModel` (loading → empty / receipts / error). Then build `ReceiptListScreen` with the empty state, rows and the "Add receipt" FAB, and replace the placeholder. Write Compose tests for the empty state and the row content. Verify the unit tests and `./gradlew :feature:receipts:connectedDebugAndroidTest` pass.
- [x] 5.5 TDD (viewmodel skill) `ReceiptFormViewModel` (add defaults: today, the default currency, Other; edit pre-fill; per-field errors; trimmed save; `savedEvent`; no save while invalid). Then build `ReceiptFormScreen` (text fields, a date picker capped at today, a category dropdown, a searchable currency picker, Save and close). Write Compose tests: validation errors appear on Save, and a valid save fires the event. Verify the unit tests and connected tests pass.
- [x] 5.6 TDD (viewmodel skill) `ReceiptDetailViewModel` (shows the receipt; delete asks for confirmation; confirm deletes and fires `closeEvent`; cancel does nothing; a `null` receipt fires `closeEvent`). Then build `ReceiptDetailScreen` with edit and delete actions and the confirmation dialog. Write Compose tests for confirm and cancel. Verify the unit tests and connected tests pass.

## 6. Documentation

- [x] 6.1 Update `README.md` (remove "starter app scaffold", list the Receipts feature, add backup exclusion to "Privacy guarantees") and `.claude/CLAUDE.md` (the `pocketreceipts.android.room` convention, and that Room schemas are exported to `data/receipts/schemas`). Verify the documented commands and plugin ids match the build.

## 7. Integration check

- [x] 7.1 Add `:app` instrumented tests: Receipts stays selected on the detail screen; detail → select Ask → back shows the list; adding, editing and deleting a receipt, one test each, against the real database; the edit form survives rotation (a real `MainActivity` recreation via `scenario.recreate()`, which retains ViewModels the way rotation does. `StateRestorationTester` doesn't, because Navigation 3 clears entry ViewModels when the composition is disposed outside a configuration change). Verify `./gradlew :app:connectedDebugAndroidTest` passes.
- [x] 7.2 From a clean build, run `./gradlew clean check assembleDebug` (all privacy checks green) and every module's `connectedDebugAndroidTest` on a device. Then check manually: a receipt survives a force-stop, a rotation mid-edit keeps the input, and with an en-GB locale a new receipt defaults to GBP. A locale without a region falls back to INR. Record the results in the change notes.

## Workflow follow-up

- Commit on `feature/receipts-manual`, push it and open a pull request into `main`. Merge only after the owner reviews and approves it (see the Git Workflow in `.claude/CLAUDE.md`).
- Archive the change with `/opsx:archive` after review. This creates `openspec/specs/receipt-management` and updates `app-navigation` and `privacy-guarantees`.
