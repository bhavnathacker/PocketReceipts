# Notes

## Task 4.1: backup exclusion check (2026-11-04)

Emulator `Pixel_10` (API 37, Google Play image, so no `adb root`), debug build, local transport (`com.android.localtransport/.LocalTransport`).

The receipt UI didn't exist yet, so stand-in files were written to the real paths with `run-as`: `databases/receipts.db`, `databases/receipts.db-wal`, `no_backup/receipts_db.key`, and a control file `files/control.txt`. Without root the backup set can't be read directly, so the check is a round trip: `bmgr backupnow`, delete the files, `bmgr restore 1`, then list what came back.

| File | Restored? | Expected |
|---|---|---|
| `files/control.txt` | Yes | Yes (proves backup and restore ran) |
| `databases/receipts.db` | No | No |
| `databases/receipts.db-wal` | No | No |
| `no_backup/receipts_db.key` | No | No |

Result: **pass** for cloud backup. Device-to-device transfer uses the same `<exclude domain="database" path="." />` rule in `<device-transfer>`, but it can't be exercised on a single emulator. That rule is checked by lint only.

Gotcha: `bmgr backupnow` reports "Backup is not allowed" (`PACKAGE_STOPPED`) for an app that has never been launched or was force-stopped. Launch it and send it to the background first.

## Task 7.2: integration check (2026-11-04)

Emulator `Pixel_10` (API 37), debug build.

**Build**: `./gradlew clean check assembleDebug` passes, including all privacy checks (`verifyNoHttpClient`, `verifyNoTrackingSdks`, `verifyInternetPermissionSources`) and `verifyModuleDependencies`. Room, SQLCipher and androidx.sqlite add no HTTP client, tracking SDK or INTERNET permission. Repeating the same task list reuses the configuration cache.

**Instrumented tests**: `./gradlew connectedDebugAndroidTest --continue`: 49 tests, 0 failures.

| Module | Suites | Tests |
|---|---|---|
| `:app` | `PocketReceiptsAppTest`, `ReceiptsFlowTest`, `ReceiptsConfigurationChangeTest` | 13 |
| `:data:receipts` | `ReceiptDaoTest`, `ReceiptsDatabaseProviderTest`, `KeystoreDatabasePassphraseProviderTest` | 15 |
| `:feature:receipts` | `ReceiptListScreenTest`, `ReceiptFormScreenTest`, `ReceiptDetailScreenTest`, `MoneyFormatterTest` | 21 |

Known issue, not caused by this change: `:data:ai` and `:data:model-download` have no instrumented tests, and their test APKs crash with "Unable to instantiate instrumentation ... AndroidJUnitRunner". `pocketreceipts.android.library` sets the runner but doesn't add the `androidx.test:runner` dependency. Modules with tests get it through `:core:ui-test` or their own build file. Follow-up: add the runner in the library convention plugin, or skip connected tests for modules without an `androidTest` source set.

**Manual checks**

| Check | Result |
|---|---|
| Add "Persist Test" $12.30, force-stop, relaunch | Pass: the receipt is listed with `$12.30`. |
| Type merchant and total in the add form, rotate to landscape | Pass: "Rotate Me" / "8.75" kept. The layout switches to the navigation rail and Receipts stays selected. |
| App locale en-GB (`cmd locale set-app-locales`), open the add form | Pass: currency preset to GBP, date shown as `4 Nov 2026`. The existing USD receipt shows as `US$12.30`. |
| Database file on disk after adding receipts | Pass: random 16-byte header (no `SQLite format 3`), merchant text not found in `receipts.db*`, and `no_backup/receipts_db.key` present. |
| Icons (task 5.1) | Pass: add, close, calendar, back, edit and delete render correctly in the list, form and detail screens. |
