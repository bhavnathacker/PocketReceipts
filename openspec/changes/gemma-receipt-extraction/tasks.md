# Tasks

## 1. Dependency setup

- [x] 1.1 Add `com.google.ai.edge.litertlm:litertlm-android` (pin 0.19.1, or the latest release at least two weeks old) to `gradle/libs.versions.toml` and as a dependency of `:data:ai`. Verify `./gradlew :data:ai:assembleDebug` succeeds. If it fails on Kotlin metadata, stop and ask the owner before changing the Kotlin version.
- [x] 1.2 Add `kotlinx-serialization-json` (catalog version) and the serialization plugin to `:data:ai`. Verify `./gradlew :data:ai:assembleDebug` succeeds.

## 2. Domain contracts

- [x] 2.1 Add `ModelState` (sealed: NotDownloaded, Downloading, Verifying, Loading, Ready, NotEnoughStorage, Failed(reason), CannotRun) and the `ModelRepository` and `ModelRuntimeRepository` interfaces to `:domain`, with KDoc. Verify `./gradlew :domain:compileKotlin` succeeds.
- [x] 2.2 Add `MonitorModelStateUseCase` (runs the one-time load test on `Loading`; collecting again after `CannotRun` retries it) and `StartModelDownloadUseCase`, with JUnit 5 tests over mocked repositories. Verify the tests pass.
- [x] 2.3 Add `ExtractedReceipt` (raw string fields) and the `ReceiptExtractionRepository` interface to `:domain`. Verify it compiles.
- [x] 2.4 Add `ReceiptFieldsChecker` (ISO date not after today; ISO 4217 currency; positive amount converted to the currency's minor units with `BigDecimal`; category by `ReceiptCategory` name; merchant trimmed and cut to `MAX_MERCHANT_LENGTH`), with one test per rule and per spec scenario of "Gemma's answer is checked". Verify the tests pass.
- [x] 2.5 Change `ReceiptScan` to single nullable values (merchant, date, total, category). Change `ScanReceiptUseCase` to recognize → delete image (still in `finally`) → extract → check, with tests for success, recognition failure, extraction failure (image still deleted) and cancellation. Verify the tests pass.
- [x] 2.6 Delete `MerchantCandidateParser`, `DateCandidateParser`, `AmountCandidateParser`, `CategoryCandidateParser`, `ReceiptScanParser` and their tests, and remove `heights` from `RecognizedText`. Verify `./gradlew :domain:test` passes and `grep -r CandidateParser domain` finds nothing.

## 3. Model download (`:data:model-download`)

- [x] 3.1 Declare `android.permission.INTERNET` in `:data:model-download`'s manifest, add the pinned model constants (URL at commit `2d74ac41…`, size 2,588,147,712, SHA-256 `1819381…a63c`), and add a `DownloadManager` wrapper behind an internal interface so it can be faked. Verify it compiles.
- [x] 3.2 Implement `DefaultModelRepository`:
  - storage check with `StatFs` (size + 100 MB);
  - enqueue into `getExternalFilesDir("models")` with a visible notification, keeping the download id in the module's `SharedPreferences`;
  - progress polled from `DownloadManager.query` as a `Flow`;
  - failures mapped to NoConnection or DownloadFailed;
  - streaming SHA-256 check, then rename on success or delete and report Corrupt;
  - a launch check that looks only at the final file's existence and size.

  Verify with unit tests over the faked download manager and a temp directory (states, corrupt file deleted, NotEnoughStorage, resume after process restart).
- [x] 3.3 Bind `ModelRepository` in a Hilt module in `:data:model-download`. Verify `./gradlew :app:assembleDebug` succeeds.

## 4. Gemma extraction (`:data:ai`)

- [x] 4.1 Add a single-thread inference dispatcher and engine creation from `ModelRepository.modelPath()` on the CPU backend, closed after each use (GPU was dropped after the real-model run: see design decision 5). Implement `ModelRuntimeRepository.canLoad` (the load test). Verify it compiles.
- [x] 4.2 Write the extraction prompt builder: instruction, today, default currency, allowed categories, numbered OCR rows, JSON-only reply. Verify with a unit test that pins the prompt for a fixed input.
- [x] 4.3 Write the reply parser: find the first `{…}`, decode it leniently into a DTO, and map it to `ExtractedReceipt`; prose or broken JSON throws an extraction error. Verify with unit tests: a clean reply, a reply wrapped in prose or code fences, nulls, extra keys, and garbage.
- [x] 4.4 Implement `DefaultReceiptExtractionRepository` (greedy sampling, output capped at about 200 tokens) and bind it with Hilt. Verify `./gradlew :app:assembleDebug` succeeds.

## 5. Model setup screen (`:feature:modelsetup`)

- [x] 5.1 Add `ModelSetupUiState` and `ModelSetupViewModel` (Hilt, StateFlow, following viewmodel-conventions) mapping each `ModelState` to UI: size and Download; percentage; Checking; Preparing; errors with Retry and the plain-words messages from the model-setup spec. Verify with Turbine ViewModel tests, one per state and action.
- [x] 5.2 Add a stateless `ModelSetupScreen` with strings in `values/strings.xml`, plus previews. Verify with Compose UI tests (`createComposeRule`): Download is shown with 2.6 GB, progress is shown, Retry calls back for each failure, and the storage message names the required space.

## 6. App gate, manifest and privacy rules

- [x] 6.1 Gate the app in `MainActivity`/`:app`: while the model state is unknown show only the background; before Ready show `ModelSetupScreen` with no navigation suite; at Ready show `PocketReceiptsApp`. Back from setup closes the app. Verify with `:app` Hilt instrumented tests using a fake `ModelRepository`: first launch shows setup without the navigation bar, Ready shows Receipts, and setup finishing shows Receipts where back leaves the app.
- [x] 6.2 Remove the unscoped INTERNET removal from `app/src/main/AndroidManifest.xml` (keep the CCT backend meta-data removal and update its comment). Add `<exclude domain="external" path="models/" />` to `backup_rules.xml` and to both sections of `data_extraction_rules.xml`. Verify the merged debug manifest contains INTERNET and no `backend:` meta-data.
- [x] 6.3 Update `PrivacyRules`: rename `INTERNET_REMOVED_BY_APP` to `INTERNET_ACCEPTED_WITHOUT_BACKEND`, accepted when the app manifest removes the CCT backend meta-data, and update `PrivacyRulesTest` for each privacy-guarantees scenario. Verify `./gradlew check` passes, and that temporarily declaring INTERNET in `:data:receipts` makes it fail.
- [x] 6.4 Update README (features: setup and the 2.6 GB download, Gemma reading scans, no chips; privacy guarantees table: INTERNET row and backup exclusion; module diagram) and the ML Kit/INTERNET note in `.claude/CLAUDE.md`. Verify the README's privacy table matches the `PrivacyRules` behavior.

## 7. Scan flow and review form

- [x] 7.1 Change `ScanPrefill` to single values (merchant, ISO date, `ScanAmount` total, category name) and update both mappers and their tests (invalid ISO date or unknown category → null). Verify the mapper tests pass.
- [x] 7.2 Update `ReceiptFormViewModel.prefilledFrom`: Gemma's values or the manual-entry defaults, and the nothing-read message when merchant, date and total are all null. Remove `dateCandidates`, `totalCandidates`, `onDateCandidateClick`, `onTotalCandidateClick`, the chip composables, their strings and tests. Verify the ViewModel tests and the `:feature:receipts` Compose tests pass (review title, prefilled values, edits survive recreation).
- [x] 7.3 Update `:feature:capture`'s reading-progress text to cover Gemma reading the receipt, and make an extraction failure show "try again / enter manually". Verify with `CaptureViewModelTest` and `CaptureScreenTest`.
- [x] 7.4 Run `./gradlew check` and the connected tests one module at a time (`:data:ocr`, `:feature:modelsetup`, `:feature:receipts`, `:feature:capture`, `:app`). Verify all pass.

## 8. Real-model verification

- [x] 8.1 On the emulator or a device, run the full setup: download, checking, then Receipts. Verify that after a restart setup isn't shown, and that turning off the network during the download shows the no-connection message with a working Retry.
- [x] 8.2 Scan the Bharat Medical Stores cash memo and the HIT-MAN receipt (photos supplied by the owner, not committed). Verify the Bharat review form shows BHARAT MEDICAL STORES, 31 Oct 2026, 764.00 INR, Health, and record the HIT-MAN result and the time per scan in the PR.

## Workflow follow-up

- Open a PR from `feature/gemma-receipt-extraction` into `main`; merge only after the owner reviews and approves.
- Archive the change with `/opsx:archive` after it is merged.
