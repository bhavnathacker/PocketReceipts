# Design

## Context

See proposal.md (Why) for motivation and `specs/receipt-capture`, `specs/receipt-management` and `specs/privacy-guarantees` for the required behaviour.

Current state (after `receipts-manual`):
- `:feature:capture` exists but is empty: only `capture.gradle.kts` (`pocketreceipts.android.feature`). There is no `:data:ocr`.
- `:feature:receipts` owns `ReceiptsKey`, `ReceiptDetailKey` and `ReceiptFormKey(receiptId: Long?)`. `ReceiptFormViewModel` is assisted-injected with `receiptId` and already handles defaults, validation, saving and configuration changes. `AmountInputFormatter` turns minor units into field text.
- `:app` owns the back stack. `TopLevelNavigator.selectedKey` is the top-level key nearest the top, so any key pushed above `ReceiptsKey` keeps Receipts selected.
- Module rules (`verifyModuleDependencies`): features depend only on `:domain` and `:core:*`, never on other features. Data modules never depend on features. Only `:app` sees everything.
- Privacy rules: only `:data:model-download` may contribute `android.permission.INTERNET`. `PrivacyRules.TRACKING_SDKS` lists banned `group:namePrefix` pairs, checked against every resolved component.
- Checked against the published artifacts:
  - `com.google.mlkit:common` 18.11.0, `vision-common` 17.3.0 and `play-services-mlkit-document-scanner` 16.0.0 depend on `com.google.android.datatransport:transport-backend-cct` 2.3.3. Its manifest declares `INTERNET` and `ACCESS_NETWORK_STATE`.
  - `transport-runtime` declares only `ACCESS_NETWORK_STATE`.
  - The ML Kit artifacts themselves declare no permissions.

## Goals / Non-Goals

**Goals:**
- Candidate parsing is pure Kotlin in `:domain`, so it can be tested quickly on the JVM, and `ai-extraction` can later reuse the same candidates and pick among them.
- One receipt form for manual entry, editing and review. No second form to keep in sync.
- No receipt images are kept, and nothing leaves the device apart from what Google Play services does itself to install the scanner.

**Non-Goals:**
- Automated end-to-end tests through the real scanner UI, which runs in Google Play services and can't be driven by the app's tests. It is checked manually (task 8.2).
- Persisting unfinished reviews beyond what the navigation back stack keeps.

## Decisions

### D1. Flow and module boundaries
```
Receipts list ──"Scan receipt"──▶ CaptureKey (:feature:capture)
   launch scanner ─▶ image URI ─▶ ScanReceiptUseCase (:domain) ─▶ ReceiptScan
        │ cancelled: pop          │ unavailable/failed: error screen (retry / add manually)
        ▼
  app replaces CaptureKey with receiptReviewKey(scan) ─▶ ReceiptFormKey(scan = …) (:feature:receipts)
```
- `:feature:receipts` doesn't know about capture, and `:feature:capture` doesn't know about receipts. `captureEntries(onScanned: (ReceiptScan) -> Unit, onAddManually: () -> Unit, onClose: () -> Unit)` reports outcomes. `receiptsEntries` gains `onScanClick: () -> Unit`, and the feature exports `receiptReviewKey(scan: ReceiptScan): NavKey`. `:app` connects them.
- `:app` *replaces* `CaptureKey` (pop then push), so back from the review form goes to the list, not to the scanner (spec: Review before saving).
- `CaptureKey` and the review key sit above `ReceiptsKey`, so Receipts stays selected without navigator changes.

*Alternative:* a separate review screen in `:feature:capture`. It would duplicate the form (date picker, category and currency pickers, amount parsing, validation display), or force those into `:core:ui` together with their domain types. Reusing the receipts form is less code, and the scan result is just a richer starting state.

### D2. Domain model and use case (`:domain`, package `...domain.scan`)
- `RecognizedText(lines: List<String>)`: visual rows, top to bottom (see D4).
- `ReceiptScan(merchant: String?, dates: List<LocalDate>, totals: List<Money>)`: every list is ordered best first, so the preselection is the first element. Lists may be empty.
- `TextRecognitionRepository { suspend fun recognize(imageUri: String): RecognizedText }` and `ScanImageRepository { suspend fun delete(imageUri: String) }`, implemented in `:data:ocr`.
- `ScanReceiptUseCase(imageUri)`: `try { recognize } finally { delete }`, then `ReceiptScanParser(lines)`. The image is deleted on success, on failure and on cancellation (spec: Scanned images are not kept).
- `ReceiptScanParser` combines three single-purpose parsers, injected with `GetTodayUseCase`, `GetDefaultCurrencyUseCase` and a `Locale` provider:
  - `DateCandidateParser`, `AmountCandidateParser`, `MerchantCandidateParser`.

### D3. Parsing rules
- **Dates**:
  - Numeric `d/m/y` with `/ . -` separators, ISO `y-m-d`, and month-name forms (`3 Feb 2026`, `Feb 3, 2026`, `03-FEB-26`).
  - Month names are English plus the device locale's (`DateFormatSymbols`).
  - Two-digit years map to 20yy.
  - When both day/month orders are valid, the locale decides. If the locale's short date pattern (`DateTimeFormatterBuilder.getLocalizedDateTimePattern(SHORT, …)`) puts `M` before `d`, month comes first. If only one order is a real date (`25/12/2026`), that order is used.
  - Dates after today are dropped. Duplicates are removed, keeping the order of first appearance.
- **Amounts**:
  - A token counts as an amount only if it has a decimal part with exactly the currency's fraction digits, or it sits next to a currency marker. This keeps out phone numbers, quantities, years and invoice numbers.
  - The decimal separator is the last `.` or `,` followed by exactly the fraction digits. Earlier separators (`,` `.` space `'`) are grouping, so `1,23,456.50`, `1.234,56` and `1 234,56` all parse without needing the locale.
  - Values are kept in minor units (`Long`).
  - **Ordering**: the last line whose label matches a total keyword wins.
    - Total keywords: `TOTAL`, `GRAND TOTAL`, `AMOUNT DUE`, `BALANCE DUE`, `NET AMOUNT`, `NET PAYABLE`, `AMOUNT PAYABLE`, `TO PAY`, `SUMME`, `GESAMT`, `TOTAL TTC`.
    - Excluded labels: `SUBTOTAL`, `SUB TOTAL`, `TOTAL TAX`, `TOTAL ITEMS`, `TOTAL QTY`, `TOTAL SAVINGS`, `TOTAL DISCOUNT`.
    - The remaining distinct amounts follow, largest first.
- **Currency**:
  - A marker on the same line as an amount sets its currency. Markers: `₹`, `Rs`, `Rs.`, `INR` → INR; `€` → EUR; `£` → GBP; `¥` → JPY; any 3-letter code `Currency.getInstance` accepts; `$` → the default currency if its symbol in the device locale is `$`, else USD.
  - With no marker, the default currency is used.
  - A JPY-style currency (0 fraction digits) needs a marker or a total label to count.
- **Merchant**: the first of the top 5 lines that has at least 3 letters and isn't mostly digits, a date, an amount, a phone number, a URL or email, or a tax-id line (`GST`, `GSTIN`, `VAT`, `TAX ID`, `UST-ID`, `TIN`). It is trimmed, internal whitespace is collapsed, and it is cut to 100 characters (the validator limit).

### D4. Text recognition (`:data:ocr`, new module)
- `pocketreceipts.android.library` + `pocketreceipts.hilt`, depends on `:domain`, `com.google.mlkit:text-recognition` 16.0.1 (Latin model bundled in the APK), and timber.
- `MlKitTextRecognitionRepository`:
  - Uses `InputImage.fromFilePath(context, uri)` and `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)`, bridged with `suspendCancellableCoroutine`, on `@IoDispatcher` (decoding the bitmap is disk and CPU work).
  - The recognizer is created per call and closed in `finally`.
- `OcrRowAssembler` (pure, JVM-tested): ML Kit often splits a receipt row ("TOTAL ........ 210.00") into separate lines or blocks.
  - All `Text.Line`s are flattened to `(text, top, bottom, left)`.
  - Lines whose vertical centres fall within half the median line height are grouped into one row.
  - Each row is sorted by `left` and joined with a space, and rows are sorted by `top`.
  - This is what makes "label + amount on one line" parsing work.
- `FileScanImageRepository.delete(uri)` deletes the file only if it is a `file:` URI inside `context.cacheDir`, where the scanner writes its results (spike: `cache/mlkit_docscan_ui_client/<n>.jpg`; no other copy is left in the app's storage). Other URIs are logged (without the path) and ignored.
- `OcrDataModule` (`internal`) binds both repositories.

### D5. Scanner (`:feature:capture`)
- Dependency: `com.google.android.gms:play-services-mlkit-document-scanner` 16.0.0.
- `GmsDocumentScannerOptions`: `setPageLimit(1)`, `setGalleryImportAllowed(true)`, `setResultFormats(RESULT_FORMAT_JPEG)`, `SCANNER_MODE_FULL`.
- `CaptureRoute` uses `rememberLauncherForActivityResult(StartIntentSenderForResult())`.
  - It launches once on entry, guarded by `rememberSaveable { launched }`, so rotation and recreation don't open the scanner twice.
  - `getStartScanIntent(activity)` failing (Play services missing or the module can't be provided) → `Unavailable`. While the module is still being installed, the spike showed that `getStartScanIntent` succeeds and the scanner activity itself shows Play services' download progress, then opens the camera; the app needs no state for that.
  - `RESULT_CANCELED` → `onClose()`.
  - `RESULT_OK` → `GmsDocumentScanningResult.fromActivityResultIntent(data).pages[0].imageUri` → `CaptureViewModel.onImageScanned(uri)`.
- `CaptureViewModel` (Hilt, `ScanReceiptUseCase`):
  - Sealed `CaptureUiState`: `Scanning` (blank screen while the scanner is open), `Reading` (progress, Cancel), `Unavailable` (message + "Add receipt manually"), `ReadFailed` (message + "Try again" + "Add manually").
  - `scannedEvent: StateEventWithContent<ReceiptScan>`.
  - Cancel cancels the reading job, and the use case's `finally` deletes the image.
- The scanner UI and image cropping run in Google Play services. The app only receives the result file.

### D6. Review mode of the receipt form (`:feature:receipts`)
- `ReceiptFormKey(receiptId: Long? = null, scan: ScanPrefill? = null)`.
  - `@Serializable ScanPrefill(merchant: String?, dates: List<String>, totals: List<ScanAmount>)` holds ISO dates and `ScanAmount(minorUnits: Long, currencyCode: String)`.
  - It lives in the key, so the candidates survive configuration changes and process death with the back stack, at no extra cost.
  - `receiptReviewKey(scan)` maps `ReceiptScan` to it with `ScanPrefillMapper`.
- `ReceiptFormViewModel.Factory.create(receiptId, scan)`:
  - With `scan`, the initial state is prefilled with merchant, `dates.first()` or today, `totals.first()` as amount text (via `AmountInputFormatter`) plus its currency, or the defaults if empty, and category Other.
  - `ReceiptFormUiState` gains `isReview`, `dateCandidates: List<LocalDate>` and `totalCandidates: List<Money>`.
  - It also gains `nothingRecognizedEvent` (triggered once when all three are empty).
- New actions:
  - `onDateCandidateClick(date)` sets the date.
  - `onTotalCandidateClick(money)` sets the amount text and currency.
  - Both clear the matching field error.
- UI:
  - Title "Review receipt".
  - A row of `FilterChip`s under the date and total fields shows the alternatives, with the current value selected. Each chip has a content description, for example "Use total ₹200.00".
  - A snackbar shows "Nothing could be read from this scan" for the empty case.
- Saving reuses `SaveReceiptUseCase` unchanged. Going back pops the key, so nothing is saved.

### D7. Entry points and navigation
- `ReceiptListScreen`:
  - The extended FAB becomes "Scan receipt" (camera icon), because scanning is the main path.
  - "Add receipt" moves to a top app bar action (add icon) and stays in the empty state.
  - The empty state shows both buttons (spec: Empty receipt list).
- `:app`:
  - `receiptsEntries(onNavigate, onBack, onScanClick = { backStack.add(CaptureKey) })`.
  - `captureEntries(onScanned = { replaceTop(receiptReviewKey(it)) }, onAddManually = { replaceTop(ReceiptFormKey()) }, onClose = { backStack.removeLastOrNull() })`.
  - Add the `:data:ocr` dependency.
- New `:core:ui` drawable: `ic_photo_camera` (Material Symbols, same style as the other icons).

### D8. Privacy: switching off ML Kit's usage logging
*Revised after the task 1.2 spike.* The first plan was to exclude `transport-backend-cct` from every configuration. On the emulator, both `GmsDocumentScanning.getClient` and `TextRecognition.getClient` then crash with `NoClassDefFoundError: com.google.android.datatransport.cct.CCTDestination`: ML Kit references the backend's classes directly. So the classes stay, and only the backend's registration and permission are removed.
- **How the backend is switched on**: `transport-backend-cct`'s manifest adds `INTERNET`, `ACCESS_NETWORK_STATE` and a `<meta-data android:name="backend:com.google.android.datatransport.cct.CctBackendFactory">` on `transport-runtime`'s `TransportBackendDiscovery` service. `transport-runtime` finds backends only through that meta-data.
- **Switch-off** in `app/src/main/AndroidManifest.xml`:
  - the `backend:…CctBackendFactory` meta-data is removed with `tools:node="remove"`. The spike showed `transport-runtime` then logs "Transport backend 'cct' is not registered" and drops the events, while scanning and OCR work normally.
  - `android.permission.INTERNET` is removed with `tools:node="remove"`. (A `tools:selector`-scoped removal left the permission in the merged manifest in the spike, so the removal is unscoped. When `:data:model-download` adds INTERNET in `model-setup`, that change must replace this removal; the INTERNET check will fail until it does, which keeps it visible.)
- **Checks** (`build-logic`, run by `check` on `:app`):
  - New `verifyNoUsageLoggingBackend`: reads each variant's merged manifest (`SingleArtifact.MERGED_MANIFEST`) and fails if any `backend:` meta-data is registered, naming it. `PrivacyRules.usageLoggingBackendViolations(mergedManifest)` holds the rule and is unit-tested.
  - `verifyInternetPermissionSources`: `PrivacyRules.INTERNET_REMOVED_BY_APP` lists `com.google.android.datatransport:transport-backend-cct`. A listed library's INTERNET is accepted only when the app manifest removes INTERNET. Any other source is still reported, removed or not.
  - `verifyNoTrackingSdks` stays as it is; `transport-backend-cct` is not on its list, because it is on the classpath on purpose.
- `ACCESS_NETWORK_STATE` from the transport libraries is harmless and is left as is.

*Alternatives:*
- Exclude `transport-backend-cct`: rejected, ML Kit crashes without it (spike).
- Allow ML Kit's logging and allowlist it in the INTERNET check: rejected by the owner, because usage stats would leave the device.
- A stub `CCTDestination` class: rejected, it depends on obfuscated ML Kit internals and could break on any update.
- CameraX plus Tesseract: rejected, because of much more code, a larger APK and weaker OCR.

### D9. Testing
- **JVM**:
  - Parser tests use synthetic OCR fixtures: en-IN (lakh grouping, `Rs.`, `GSTIN`, day-first), en-US (month-first, `$`), en-GB (`£`), de-DE (`1.234,56`, `SUMME`) and JPY (`¥1,500`). They cover labelled versus unlabelled totals, subtotal and tax exclusion, future dates and the merchant rules.
  - `OcrRowAssembler` tests.
  - `ScanReceiptUseCase` tests: the image is deleted on success, on failure and on cancellation.
  - `CaptureViewModel`, and the review additions to `ReceiptFormViewModel`.
  - `PrivacyRules` tests for the backend registration and the INTERNET exception.
  - One behavior per test (project rule).
- **Instrumented**:
  - `:data:ocr`: draw known receipt text onto a `Bitmap`, save it to `cacheDir`, and check that `MlKitTextRecognitionRepository` returns rows with the label and amount joined. Also check that `FileScanImageRepository` deletes it. This runs the real bundled model with no binary test assets.
  - `:feature:capture`: `CaptureScreen` states (reading + cancel, unavailable + add manually, read failed + retry).
  - `:feature:receipts`: review title, candidate chips switching date and total, and the empty-scan message.
  - `:app`: a review key pushed above Receipts keeps Receipts selected, and back from review returns to the list.

## Risks / Trade-offs

- [ML Kit crashes without `transport-backend-cct`] → Confirmed by task 1.2; D8 was revised with the owner to keep the classes and remove the backend's registration instead.
- [A future ML Kit or transport version registers its backend differently] → `verifyNoUsageLoggingBackend` checks the merged manifest for any `backend:` registration, so a new registration fails the build.
- [The scanner may write results outside `cacheDir`, or keep its own copies] → Task 1.2 found results in `cacheDir/mlkit_docscan_ui_client/` and no other copies, so D4's rule stands.
- [The scanner module isn't installed yet, or the device has no Play services] → `Unavailable` with manual entry. First use needs Play services to download the module (its network, not the app's).
- [OCR on faded thermal paper is unreliable, and the heuristics will pick the wrong total or merchant sometimes] → Every value is shown for confirmation, alternatives are one tap away, and `ai-extraction` improves the selection later. Parser fixtures keep regressions visible.
- [The bundled Latin model adds about 4 MB per ABI] → Accepted for offline OCR. ABI splits come in release-readiness.
- [Large candidate lists in the nav key] → Keep at most 5 dates and 8 totals.
- [Non-Latin receipts (Devanagari, CJK) read poorly] → Out of scope. Numbers and Latin labels still usually read.

## Migration Plan

No data migration: the Room schema is unchanged. Rollback is a revert. Hiding the scan action alone would leave unused code, so revert the whole change instead.

## Open Questions

- Whether `SCANNER_MODE_FULL` needs a larger module download than `SCANNER_MODE_BASE_WITH_FILTER` wasn't measured: the emulator downloaded the module once for FULL. It is a one-time download by Play services, so FULL stays.
