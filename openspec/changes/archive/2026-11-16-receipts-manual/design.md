# Design

## Context

See proposal.md (Why) for motivation and `specs/receipt-management`, `specs/app-navigation` and `specs/privacy-guarantees` for the required behaviour.

Current state (after `foundation`):
- `:domain` is a pure JVM module (`javax.inject` and coroutines only), holding the `@IoDispatcher` / `@DefaultDispatcher` qualifiers.
- `:data:receipts` is an empty Android library module (`pocketreceipts.android.library` + `pocketreceipts.hilt`, depends on `:domain`).
- `:feature:receipts` exposes `ReceiptsKey` and `receiptsEntries()`, which shows a `PlaceholderScreen`.
- `:app` owns the single back stack (`rememberNavBackStack(ReceiptsKey)`), `TopLevelNavigator` and `NavDisplay`, which already has the saveable-state and ViewModel-store entry decorators. `DispatcherModule` lives in `:app`.
- `backup_rules.xml` and `data_extraction_rules.xml` are still the template files with nothing excluded.
- Room and a date/time abstraction don't exist yet. minSdk is 26, so `java.time` is available without desugaring.

## Goals / Non-Goals

**Goals:**
- A receipt model and repository API that capture/AI extraction (saving a reviewed draft) and Ask (querying by date range, category and currency) can use without changing them.
- Receipt data never leaves the phone: no backup or device transfer of the database.
- No disk work on the main thread.

**Non-Goals:**
- Unsaved form input surviving process death (configuration changes only, per spec). Saving drafts belongs with capture.
- Encryption at rest (see D5), or exporting and importing data.
- Search, filters, paging. The list loads all receipts. Paging comes later, if volume calls for it.
- Room migrations (schema v1 only; exporting the schema now makes later migrations testable).

## Decisions

### D1. Domain model (`:domain`, package `...domain.receipt`)
- `Money(minorUnits: Long, currencyCode: String)`: integer minor units. `currencyCode` is an ISO 4217 code. The number of decimal places comes from `java.util.Currency.defaultFractionDigits`. No `Double` anywhere, so "Amounts are exact" holds by construction.
- `ReceiptCategory` enum: the nine categories in spec order.
- `Receipt(id: Long, merchant: String, purchaseDate: LocalDate, total: Money, category: ReceiptCategory, note: String?)`.
- `ReceiptInput` holds the same fields without `id` and is what the form and the later review form save.
- `ReceiptRepository`:
  - `monitorReceipts(): Flow<List<Receipt>>` (spec order: date desc, then created desc)
  - `monitorReceipt(id): Flow<Receipt?>` (`null` once deleted)
  - `addReceipt(input): Long`, `updateReceipt(id, input)`, `deleteReceipt(id)`
- Use cases (one each, per conventions): `MonitorReceiptsUseCase`, `MonitorReceiptUseCase`, `SaveReceiptUseCase(id: Long?, input)`, `DeleteReceiptUseCase`, `GetDefaultCurrencyUseCase`.
- `SaveReceiptUseCase` trims the input, validates it with `ReceiptValidator`, and returns `SaveReceiptResult.Saved(id)` or `SaveReceiptResult.Invalid(errors: Set<ReceiptFieldError>)`. Validation lives in the domain so the later capture review form gets the same rules for free.
- "Today" comes from an injected `java.time.Clock`, provided by a `TimeModule` in `:app` (system default zone). Tests pass a fixed clock.
- `GetDefaultCurrencyUseCase` returns `Currency.getInstance(Locale.getDefault())`, or INR when the locale has no region or currency. These are plain JVM APIs, so the domain stays Android-free.

*Alternative:* `BigDecimal` in the model. It's exact too, but it's awkward in Room and in SQL sums for Ask. Minor units in a `Long` sum directly in SQL.

### D2. Amount input and display (`:feature:receipts`, presentation)
- `AmountInputParser(text, currencyCode, locale) → Long?` accepts the locale's decimal separator and rejects more decimal places than the currency has. JPY accepts none. It works through `BigDecimal` and then converts to minor units.
- It also accepts the locale's grouping separator, in any grouping pattern: en-IN lakh grouping `1,23,456.50` and plain `123456.50` both give `12345650`. Grouping separators are removed before parsing, and only digits, at most one decimal separator, and grouping separators before it are allowed. Anything else (`1.2.3`, a grouping separator after the decimal separator, letters) is rejected.
- `MoneyFormatter` (a UI mapper) uses Android's ICU `android.icu.text.NumberFormat.getCurrencyInstance(locale)` with the receipt's currency and its fraction digits. It doesn't use `java.text.NumberFormat`, because that only supports uniform grouping and prints `₹123,456.50` instead of `₹1,23,456.50` in en-IN.
- `AmountInputParser` is a pure JVM class with mapper-style JVM tests. `MoneyFormatter` needs Android ICU, so its tests run on a device. Mappers that use it mock it in JVM tests.

### D3. Room convention plugin `pocketreceipts.android.room`
- Applies `androidx.room` (the Room Gradle plugin) and KSP, sets `room { schemaDirectory("$projectDir/schemas") }`, and adds `room-runtime` and `room-ktx` (if the chosen Room version still ships it separately) plus `ksp(room-compiler)`.
- It's registered in `convention.gradle.kts` next to the others, with `compileOnly(room-gradlePlugin)`. It must stay configuration-cache safe.
- Only `:data:receipts` applies it. The exported `schemas/` folder is committed.

### D4. Schema v1 (`:data:receipts`)
Table `receipts`:

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK autoincrement | |
| `merchant` | TEXT NOT NULL | trimmed |
| `purchase_date` | INTEGER NOT NULL | `LocalDate.toEpochDay()`, sortable and easy to range-query for Ask |
| `total_minor` | INTEGER NOT NULL | |
| `currency_code` | TEXT NOT NULL | ISO 4217 |
| `category` | TEXT NOT NULL | stable key (`groceries`, …), not the enum name or ordinal |
| `note` | TEXT NULL | |
| `created_at` / `updated_at` | INTEGER NOT NULL | epoch millis from the injected `Clock`. `created_at` breaks ties in list ordering |

- Index on `(purchase_date, created_at)`.
- The entity uses primitives only, so there are no Room `TypeConverter`s. `ReceiptEntityMapper` and `ReceiptCategoryMapper` (data layer) convert to and from the domain. An unknown category key maps to `OTHER` and is logged, never a crash.

### D5. No encryption at rest
- The database is a plain Room database: `Room.databaseBuilder(context, ReceiptsDatabase::class.java, "receipts.db")`, provided as a Hilt singleton. Building it doesn't touch disk; Room opens the file on the first query, and every DAO method is `suspend` or returns a `Flow`, so that happens off the main thread.
- The repository uses the DAO directly. If a query fails, the error reaches the caller through the flow or suspend call; the list ViewModel shows the error state ("Your receipts couldn't be opened").
- Encryption (SQLCipher with a Keystore-wrapped passphrase) was implemented first and then removed at the owner's request to keep the implementation simple. The data stays on the device (D6) and inside the app sandbox.

*Trade-off:* a database file copied off a rooted or compromised device is readable. Adding encryption later means migrating the existing plaintext file, for example with SQLCipher's `sqlcipher_export`.

### D6. Backup exclusion
- `backup_rules.xml` (API ≤ 30) and both `<cloud-backup>` and `<device-transfer>` in `data_extraction_rules.xml` (API 31+) exclude the `database` domain, so receipt data never leaves the phone through backup or transfer.
- release-readiness may tighten backup further, for example with `allowBackup="false"`. This change covers only the receipt data.

### D7. Hilt wiring
- `:data:receipts` has an `internal` `ReceiptsDataModule` (`@InstallIn(SingletonComponent::class)`) that binds `DefaultReceiptRepository` to `ReceiptRepository`, provides `ReceiptsDatabase` as a singleton and `ReceiptDao` from it.
- `:app` picks the module up through its existing dependency on `:data:receipts`, so it stays the composition root without new code apart from `TimeModule`.

### D8. Navigation (spec: app-navigation)
- `:feature:receipts` adds `@Serializable data class ReceiptDetailKey(val receiptId: Long) : NavKey` and `@Serializable data class ReceiptFormKey(val receiptId: Long? = null) : NavKey` (null means add).
- `receiptsEntries(onNavigate: (NavKey) -> Unit, onBack: () -> Unit)`. The feature decides which key to push; `:app` passes `backStack::add` and `{ backStack.removeLastOrNull() }`. Features still never reference each other.
- `TopLevelNavigator` already does what the spec needs. `selectedKey` ignores non-top-level keys, so Receipts stays selected on detail and form. `select(Ask)` trims the stack to `[Receipts]` before pushing, so nested screens are dropped. No code change is needed, only new unit-test cases to pin this down.
- After a successful save, the form pops itself, returning to the detail screen (edit) or the list (add). After a confirmed delete, the detail screen pops itself. If a detail screen's receipt becomes `null` (deleted elsewhere), it pops too.
- Nested keys are `@Serializable`, so `rememberNavBackStack` restores them after rotation and process death.

### D9. Presentation (`:feature:receipts`)
Per the ViewModel conventions (`internal`, a `MutableStateFlow` exposed as `StateFlow`, use cases only, `StateEvent` for one-shot events):
- `ReceiptListViewModel`: `uiState` is loading, empty, receipts (as `ReceiptListItemUiModel` from `ReceiptListItemUiMapper`) or error.
- `ReceiptDetailViewModel` (assisted `receiptId`): the receipt, the delete-confirmation dialog flag, and a `closeEvent`.
- `ReceiptFormViewModel` (assisted `receiptId: Long?`): field values as raw strings and a `LocalDate`, per-field errors, `isSaving`, and a `savedEvent`. In edit mode it loads the receipt once. `onSave` parses the amount (D2) and calls `SaveReceiptUseCase`, mapping each `ReceiptFieldError` to an error string resource.
- Screens use Material 3: a list with an extended "Add receipt" FAB (also the empty-state action), a detail screen with a top app bar holding back, edit and delete actions, and a form with a top app bar holding close and Save, then text fields, a `DatePickerDialog` whose `SelectableDates` stop at today, a category dropdown, and a searchable currency picker over `Currency.getAvailableCurrencies()` sorted by code. All strings are resources. Content insets are applied as in foundation.
- New icons (add, edit, delete, back, close) are Material Symbols vector drawables in `:core:ui` `AppIcons`, following foundation D6.

### D10. Testing (TDD order, per project test conventions)
- **JVM (JUnit 5, Mockito, Turbine, Truth)**:
  - `ReceiptValidator`, every use case, `AmountInputParser`, `MoneyFormatter`, the entity and category mappers, `ReceiptListItemUiMapper`
  - `DefaultReceiptRepository` with a mocked DAO and provider
  - the three ViewModels
  - new `TopLevelNavigator` cases for nested keys
- **Instrumented (JUnit 4)**:
  - **`:data:receipts`**: DAO tests on an in-memory Room database (ordering with ties, update, delete, `observeById` emits `null` after delete).
  - **`:feature:receipts`**: Compose tests for the empty state, the list, form validation errors, and the delete confirmation.
  - **`:app`**: navigation tests (Receipts stays selected on detail; tab switch then back shows the list) and add, edit and delete flows, one per test, against the real database.

## Risks / Trade-offs

- [Room and KSP with AGP 9 built-in Kotlin are a fresh combination] → The first task adds the dependencies and the convention plugin and builds an empty database before any feature work.
- [The database is not encrypted, so a copy taken from a rooted device is readable] → Accepted to keep the implementation simple (D5). Android's sandbox and file-based encryption still protect it on a normal device.
- [Backups are excluded, so uninstalling or losing the phone loses the receipts] → Accepted for the privacy promise. Export comes later.
- [Loading the whole list doesn't scale to thousands of receipts] → Fine for MVP volumes. The DAO API can move to Paging later without changing the domain repository contract much.
- [Locale-dependent amount parsing (`,` vs `.`)] → The parser takes the locale explicitly and is tested with en-US, de-DE, en-IN (lakh grouping) and ja-JP.

## Migration Plan

The database and screens are new, and nothing has been released, so there is no data to migrate. The schema starts at version 1 with export enabled, so later changes can write and test migrations. Rollback is reverting the change's commits.

## Open Questions

- Exact versions of Room and the Room Gradle plugin: the newest stable releases compatible with AGP 9.3, Kotlin 2.2 and KSP 2.3 at implementation time. This doesn't affect the approach.
