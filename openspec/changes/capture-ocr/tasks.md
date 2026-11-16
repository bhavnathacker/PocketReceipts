# Tasks

## 1. Dependencies and privacy guard

- [x] 1.1 Add catalog entries for `com.google.android.gms:play-services-mlkit-document-scanner` 16.0.0 and `com.google.mlkit:text-recognition` 16.0.1.
- [x] 1.2 Device spike (throwaway code on this branch, removed before 2.1): launch the scanner from a debug-only button, run text recognition on the result, and log the row count. Confirm on the emulator that:
  - scanning and OCR work with the backend switched off, with no crash and no upload attempts in logcat
  - the merged manifest has no INTERNET from ML Kit
  
  Record in the change notes where the scanner writes result files, whether it keeps other copies, whether `SCANNER_MODE_FULL` needs a larger module download than `BASE_WITH_FILTER`, and what `getStartScanIntent` returns while the module is still installing. Adjust D4/D5 if any answer differs.
- [x] 1.3 Switch off ML Kit's usage logging (D8, revised): in `app/src/main/AndroidManifest.xml` remove the `backend:com.google.android.datatransport.cct.CctBackendFactory` meta-data and the INTERNET permission. TDD `PrivacyRules.usageLoggingBackendViolations` and the `INTERNET_REMOVED_BY_APP` exception in `internetPermissionViolations`, then add the `verifyNoUsageLoggingBackend` task on `:app` (both variants' merged manifests, part of `check`). Verify `./gradlew :build-logic:convention:test check` passes. Temporarily drop each manifest removal and confirm the matching check fails, naming the backend.

## 2. Domain: scan models and parsers (`:domain`)

- [x] 2.1 Add `RecognizedText`, `ReceiptScan`, `TextRecognitionRepository` and `ScanImageRepository` in `...domain.scan` (D2), with KDoc. Verify `./gradlew :domain:build` passes.
- [x] 2.2 TDD `DateCandidateParser` (D3): numeric, ISO and month-name forms; locale order decides an ambiguous `03/02/2026` (en-IN → 3 Feb, en-US → 2 Mar); a date valid in only one order uses that order; two-digit years; future dates dropped; duplicates removed in order; at most 5. Verify `./gradlew :domain:test` passes.
- [x] 2.3 TDD `AmountCandidateParser` (D3): `1,23,456.50`, `1.234,56` and `1 234,56` parse to minor units; phone numbers, years and quantities aren't amounts; the last labelled total comes first and excluded labels (SUBTOTAL, TOTAL TAX, …) don't count; then largest first, at most 8. Currency markers `₹`/`Rs.`/`INR`, `€`, `£`, `¥` (0 decimals), ISO codes, and `$` resolving by default currency. Verify `./gradlew :domain:test` passes.
- [x] 2.4 TDD `MerchantCandidateParser` (D3): the first name-like line among the top 5; skips dates, amounts, phone, URL/email and tax-id lines; collapses whitespace; at most 100 characters; null when there is none. Verify `./gradlew :domain:test` passes.
- [x] 2.5 TDD `ReceiptScanParser` with the fixtures from D9 (en-IN, en-US, en-GB, de-DE, JPY), one test per expected field, and (usecase skill) `ScanReceiptUseCase`: it returns the parsed scan and deletes the image on success, on recognition failure and on cancellation. Verify `./gradlew :domain:test` passes.

## 3. Text recognition (`:data:ocr`)

- [x] 3.1 Create `:data:ocr` (`includeModule`, `pocketreceipts.android.library` + `pocketreceipts.hilt`, depends on `:domain`, `text-recognition`, timber) and add it to `:app`. Verify `./gradlew check` passes, including `verifyModuleDependencies` and the privacy checks.
- [x] 3.2 TDD `OcrRowAssembler` on the JVM (D4): lines on the same visual row are joined left to right, rows are sorted top to bottom, and a slightly skewed row still groups. Verify `./gradlew :data:ocr:testDebugUnitTest` passes.
- [x] 3.3 Implement `MlKitTextRecognitionRepository` (on `@IoDispatcher`, recognizer closed in `finally`), `FileScanImageRepository` (deletes only `file:` URIs inside `cacheDir`, or the location found in 1.2), and the internal `OcrDataModule`. Add instrumented tests that draw "TOTAL" and "210.00" at both ends of one row onto a bitmap in `cacheDir`, check that recognition returns a row containing both, and check that delete removes the file and ignores a URI outside the cache. Verify `./gradlew :data:ocr:connectedDebugAndroidTest` passes.

## 4. Capture feature (`:feature:capture`)

- [x] 4.1 Add `CaptureKey`, `CaptureUiState` (`Scanning`, `Reading`, `Unavailable`, `ReadFailed`) and, with TDD (viewmodel skill), `CaptureViewModel`:
  - `onImageScanned` moves to Reading and fires `scannedEvent` with the scan
  - a failure moves to ReadFailed
  - `onCancel` cancels reading and fires `closeEvent`
  - `onScannerUnavailable` moves to Unavailable
  - `onRetry` moves back to Scanning
  
  Verify `./gradlew :feature:capture:testDebugUnitTest` passes.
- [x] 4.2 Implement `CaptureScreen` (stateless: progress with Cancel, Unavailable with "Add receipt manually", ReadFailed with "Try again" and "Add manually"), `CaptureRoute` (scanner options from D5, launched once via `rememberSaveable`, result handling) and `captureEntries(onScanned, onAddManually, onClose)`, with strings and previews. Add Compose tests, one behavior each. Verify `./gradlew :feature:capture:connectedDebugAndroidTest` passes.

## 5. Review mode and entry points (`:feature:receipts`, `:core:ui`)

- [x] 5.1 Add `ScanPrefill`/`ScanAmount` to `ReceiptFormKey`, `receiptReviewKey(scan)`, and (mapper skill, TDD) `ScanPrefillMapper` both ways, with the D6 caps. Verify `./gradlew :feature:receipts:testDebugUnitTest` passes.
- [x] 5.2 TDD (viewmodel skill) the review additions to `ReceiptFormViewModel`:
  - prefilled merchant, date, total and currency
  - defaults when lists are empty
  - `isReview`
  - `nothingRecognizedEvent` only when all are empty
  - `onDateCandidateClick` and `onTotalCandidateClick`, which also clear the field errors
  - saving through `SaveReceiptUseCase` as an add
  
  Add `onScanClick` to `receiptsEntries`. Verify `./gradlew :feature:receipts:testDebugUnitTest` passes.
- [x] 5.3 Add the `ic_photo_camera` drawable to `:core:ui` (`AppIcons.Camera`). Update `ReceiptListScreen` (D7: "Scan receipt" FAB, add action in the top app bar, both actions in the empty state) and `ReceiptFormScreen` (title "Review receipt", candidate `FilterChip` rows with content descriptions, empty-scan snackbar). Add Compose tests, one behavior each. Verify `./gradlew :feature:receipts:connectedDebugAndroidTest` passes.

## 6. App wiring (`:app`)

- [x] 6.1 Wire `receiptsEntries(onScanClick)` and `captureEntries` in `PocketReceiptsApp` (D7: the capture key is replaced by the review or form key, and close pops it). Add `:app` instrumented tests:
  - Receipts stays selected on capture and review keys
  - back from review shows the list
  - saving from review adds the receipt
  
  Verify `./gradlew :app:connectedDebugAndroidTest` passes.

## 7. Documentation

- [x] 7.1 Update `README.md`: the scan feature, the Google Play services requirement for scanning, scanned images not being kept, and ML Kit usage logging being switched off under "Privacy guarantees" (with the new `verifyNoUsageLoggingBackend` row). Update `.claude/CLAUDE.md`: the `:data:ocr` module, the app-manifest removals that switch off `transport-backend-cct`, and "never remove them". Verify the documented module and plugin names match the build.

## 8. Verification

- [x] 8.1 From a clean build, run `./gradlew clean check assembleDebug` (all privacy checks green, merged manifest without INTERNET or an upload backend) and the connected tests of `:app`, `:data:receipts`, `:data:ocr`, `:feature:receipts` and `:feature:capture` on the emulator. Record the counts in the change notes.
- [x] 8.2 Manual checks on the emulator, recorded in the change notes:
  - scan a printed or on-screen receipt with the camera, and import one from the gallery; the review is prefilled
  - switch the total via a chip and save; the receipt is listed
  - cancel the scanner; back from review
  - rotate during review; the chosen candidate is kept
  - after a scan, no scanner image remains in the app's storage (`run-as … ls -R cache`)
  - in airplane mode, a scan still reads (once the module is installed)
  - app size difference noted

## Workflow follow-up

- Open a PR into `main` for owner review. After merge, run `/opsx:archive`.
