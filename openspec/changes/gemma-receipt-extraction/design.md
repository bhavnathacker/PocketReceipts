# Design

## Context

See proposal.md for why. The current state that shapes the approach:

- **Scan path:** `CaptureViewModel` calls `ScanReceiptUseCase`, which does three steps:
  1. `TextRecognitionRepository.recognize` (ML Kit, in `:data:ocr`) returns `RecognizedText` (rows in reading order, plus row heights).
  2. It deletes the image.
  3. `ReceiptScanParser` (four rule-based parsers in `:domain`) builds `ReceiptScan(merchant, dates, totals, category)`.
- **Hand-off to the form:** the scan travels to the review form as `ScanPrefill` inside the Navigation 3 key. The form shows date and total candidate chips.
- **Empty modules:** `:data:ai`, `:data:model-download` and `:feature:modelsetup` exist with only a build file. `:app` already depends on all three.
- **Privacy build checks:**
  - `PrivacyRules` confines HTTP client libraries and INTERNET to `:data:model-download`.
  - The app manifest removes INTERNET unscoped, so ML Kit's data-transport library can stay. A `tools:selector`-scoped removal was tried before and didn't strip the permission.
  - `verifyNoUsageLoggingBackend` fails if the CCT backend meta-data is in the merged manifest.
- **Backup:** Auto Backup and device transfer exclude only the `database` domain. Auto Backup gives up on an app whose data exceeds 25 MB, so a 2.6 GB file in backed-up storage would break backups.
- **Versions:** Hilt is at 2.60.1, which LiteRT-LM requires. The catalog's Kotlin version is 2.2.10. minSdk is 26; LiteRT-LM needs 24.

## Goals / Non-Goals

**Goals:**
- One model (Gemma 4 E2B) and one extraction path, with no rule-based fallback.
- The download survives the app being backgrounded or killed, and needs no HTTP client library.
- The app opens straight to Receipts on every launch after setup. Setup is not shown again, and the model is not reloaded at startup.
- Gemma's output is treated as untrusted: parsed defensively and checked against the form's own rules.

**Non-Goals:**
- Giving Gemma the image (multimodal) to read handwriting. Gemma only sees ML Kit's text.
- Using Gemma for Ask. This design only puts the engine in `:data:ai` where Ask can reuse it later.
- Model updates, deleting or re-downloading the model from Settings, choosing a model, or Wi-Fi-only downloads.
- NPU builds of the model.

## Decisions

### 1. Model file: pinned Gemma 4 E2B, CPU/GPU build

- **File:** `gemma-4-E2B-it.litertlm` from `litert-community/gemma-4-E2B-it-litert-lm` (Apache 2.0, not gated, no token needed).
- **Pinned URL:** `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/2d74ac41a33d7da82f39f7d5720a04c68514a103/gemma-4-E2B-it.litertlm`.
- **Expected size and checksum:** 2,588,147,712 bytes; SHA-256 `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`.
- These three values are constants in `:data:model-download`.
- **Why pin a commit:** a moving `main` could change the file under a fixed checksum, and every download would then fail verification.
- **Alternatives:**
  - *Gemma 3 1B:* smaller, but weaker at reading messy OCR text.
  - *The `-gpu` build:* GPU only.
  - *NPU builds:* tied to one chip.

### 2. Download with `DownloadManager` into app-specific external storage

- **How:** `:data:model-download` enqueues a `DownloadManager` request into `getExternalFilesDir("models")`, with a visible notification and any network type.
  - The system process does the transfer. It resumes after network drops and keeps going when the app is killed, with no foreground service and no HTTP library (`PrivacyRules.HTTP_CLIENT_GROUPS` stays untouched).
  - `DownloadManager` requires the app to hold INTERNET, which `:data:model-download`'s manifest declares.
- **Download id:** stored in the module's own `SharedPreferences`. On launch, `DownloadManager.query` returns the download's status and bytes.
- **Storage check:** before enqueueing, `StatFs` on the target directory must show the expected size plus 100 MB free. Otherwise the state is `NotEnoughStorage(requiredBytes)`.
- **Alternatives:**
  - *OkHttp in a foreground `WorkManager` worker:* more code, plus resume handling.
  - *Play for On-Device AI:* needs Play Console distribution, which this app doesn't use.

### 3. Verify once, then rename; "ready" means the verified file exists

- **On completion:** the file is streamed through SHA-256 on the IO dispatcher. That takes several seconds and shows as a "Checking download" state.
- **If it matches:** it is renamed to its final name `gemma-4-E2B-it.litertlm`.
- **If it doesn't:** it is deleted and the state is `Failed(Corrupt)`.
- **Launch check:** the app checks that the final file exists with the expected size. It doesn't re-hash 2.6 GB on every launch.
- **Load test:** right after verification, setup loads the engine once (decision 5) to prove the phone can run it.
  - Success sets a `modelLoadable` flag, and the app opens.
  - Failure gives `CannotRun`, and Retry repeats the load test (by collecting `MonitorModelStateUseCase` again).

### 4. Domain contracts (Clean Architecture)

`:domain` gets three repository interfaces:
- `ModelRepository` (`:data:model-download`): `monitorModelState(): Flow<ModelState>` (emits `Loading` for a verified file not yet load-tested, `Ready` after `markLoadVerified()`), `startDownload()`, `markLoadVerified()`, and `modelPath(): String?`.
- `ModelRuntimeRepository` (`:data:ai`): `canLoad(modelPath): Boolean`, the one-time load test.
- `ReceiptExtractionRepository`: `extract(text: RecognizedText, today: LocalDate, defaultCurrency: String): ExtractedReceipt` (raw fields as strings).

`ModelState` is a sealed type with these states:
- `NotDownloaded(sizeBytes)`
- `Downloading(downloadedBytes, totalBytes)`
- `Verifying`
- `Loading`
- `Ready`
- `NotEnoughStorage(requiredBytes)`
- `Failed(reason: NoConnection | DownloadFailed | Corrupt)`
- `CannotRun`

Use cases:
- `MonitorModelStateUseCase`: on `Loading` it runs the load test, then marks it verified or emits `CannotRun`. Retry after `CannotRun` collects it again, so no separate retry use case is needed.
- `StartModelDownloadUseCase`.
- `ScanReceiptUseCase` changes to: recognize → delete image → extract → check.

The checking step (`ReceiptFieldsChecker` in `:domain`) turns `ExtractedReceipt` into `ReceiptScan(merchant, date, total, category)`, all nullable, by applying the "Gemma's answer is checked" rules:
- ISO date, not after today.
- Currency in `Currency.getAvailableCurrencies()`.
- Amount parsed with `BigDecimal` into the currency's minor units, positive.
- Category is a `ReceiptCategory` name.
- Merchant cut to `ReceiptValidator.MAX_MERCHANT_LENGTH`.

`:data:ai` reads the model path through the domain `ModelRepository`, not by depending on `:data:model-download`. Data modules don't depend on each other, and `verifyModuleDependencies` enforces that.

### 5. LiteRT-LM engine: load per scan, GPU then CPU

- **Load per scan:** `:data:ai` creates the `Engine` when a scan needs it and closes it when extraction ends.
  - Keeping 0.8–1.7 GB resident while the user browses the list isn't worth it for an occasional scan.
  - The price is a few seconds of load time per scan, which the reading progress covers.
- **Backend:** GPU is tried first, because it is faster and lighter on memory per the model card. If GPU initialization throws, the CPU backend is used.
  - This is a choice of backend, not a fallback to rules: no model still means no scan.
  - The `uses-native-library` entries for OpenCL go in `:data:ai`'s manifest, `required="false"`.
- **Off the main thread:** all engine work runs on a dedicated single-thread dispatcher.
- **Alternative:** a long-lived singleton with an idle timeout. Revisit when Ask uses the engine too.

### 6. Prompt and output

- **Prompt:** one user turn containing:
  - the instruction;
  - today's date and the device's default currency, so Gemma can resolve two-digit years and bare amounts;
  - the allowed category names;
  - the OCR rows, numbered, in reading order.
- **Output format:** a single JSON object only: `{"merchant": string|null, "date": "YYYY-MM-DD"|null, "total": "1234.56"|null, "currency": "INR"|null, "category": "HEALTH"|null}`.
- **Instruction rules:**
  - The total is the amount actually paid (net or payable, not gross).
  - The merchant is the shop's name, not an address or a document title.
  - Return null rather than guess.
- **Sampling:** greedy (top-k 1), with output capped at about 200 tokens.
- **Parsing:** `kotlinx.serialization` (already in the catalog) decodes the first `{…}` block of the reply into a lenient DTO, with unknown keys ignored. Anything that doesn't parse is an extraction failure, and the user gets "try again / enter manually".
- **Prompt location:** the prompt text is a constant in `:data:ai`, with a unit test that pins it.

### 7. App gate in `:app`

- `MainActivity` hosts a top-level composable that collects `ModelSetupViewModel.uiState`, which lives in `:feature:modelsetup` and uses the model use cases.
  - While the state is unknown (first emission), the app shows only the window background, so there's no flash of setup.
  - Before `Ready`, it shows `ModelSetupScreen` with no `NavigationSuiteScaffold`.
  - At `Ready`, it shows today's `PocketReceiptsApp` shell, starting on Receipts.
- Because setup is not a `NavKey` on the back stack, back from setup closes the activity, and back from Receipts after setup leaves the app. The existing navigation tests stay valid.

### 8. Manifest and privacy rules

- **App manifest:**
  - Delete the unscoped INTERNET removal from `app/src/main/AndroidManifest.xml`.
  - Keep the `CctBackendFactory` meta-data removal.
  - `:data:model-download`'s manifest declares INTERNET.
- **`PrivacyRules.internetPermissionViolations`:**
  - Accepts a library in the renamed `INTERNET_ACCEPTED_WITHOUT_BACKEND` set when the app manifest removes `backend:com.google.android.datatransport.cct.CctBackendFactory`.
  - Keeps failing for INTERNET declared by any other source.
- **`verifyNoUsageLoggingBackend`:** unchanged; it still guards the merged manifest.
- **Backup rules:** add `<exclude domain="external" path="models/" />` to both `backup_rules.xml` and `data_extraction_rules.xml` (cloud backup and device transfer).
- **Docs:** update README and the `CLAUDE.md` build-setup note.

### 9. Removals and simplifications

- **Deleted from `:domain`:** `MerchantCandidateParser`, `DateCandidateParser`, `AmountCandidateParser`, `CategoryCandidateParser`, `ReceiptScanParser` and their tests.
- **`RecognizedText`:** keeps `lines`. `heights` is removed, since only the merchant rule used it. `OcrRowAssembler` keeps joining rows in reading order, which helps Gemma.
- **`ReceiptScan`:** becomes single values.
- **`ScanPrefill`:** holds `merchant`, `date` (ISO string), `total` (`ScanAmount`) and `category`, all nullable.
- **Review form:**
  - Removed: `dateCandidates`, `totalCandidates`, `onDateCandidateClick`, `onTotalCandidateClick`, the chip rows and their strings and tests.
  - Kept: the "nothing could be read" message, shown when merchant, date and total are all null.

## Risks / Trade-offs

- **[Kotlin metadata]** `litertlm-android` may be compiled with a newer Kotlin than the project's 2.2.10, giving a "binary version of its metadata is 2.x" error. → Add the dependency first and build (task 1.1). If it fails, raise it with the owner before bumping Kotlin; don't change toolchain versions silently.
- **[Low-RAM phones]** Phones under about 6 GB RAM may fail to load the model, or be killed while reading. → The load test at setup gives `CannotRun` up front, and a crash during a scan surfaces as "Gemma fails". This is accepted: the owner chose no fallback.
- **[Download size]** 2.6 GB on mobile data is expensive. → Setup states the size before the user taps Download, and the system notification shows progress. Wi-Fi-only is a non-goal for now.
- **[Hallucination]** Gemma can invent a total or misread a date. → The checker drops invalid values, the prompt asks for null rather than guesses, and the user reviews every field before saving.
- **[Slower scans]** Engine load plus inference takes a few seconds per scan. → One progress state covers OCR and extraction, and the user can cancel.
- **[No CI coverage of the real model]** CI can't download 2.6 GB. → Unit tests cover the prompt, parsing and checking with fixed replies. A manual task runs the real model on the emulator against the two real receipts (Bharat Medical Stores, HIT-MAN), with the file pushed by `adb`.
- **[Checksum cost]** Hashing 2.6 GB takes several seconds once, after download. → It runs only once, with its own state.

## Migration Plan

1. Build and test the new modules and the gate, then remove the parsers in the same PR. There's one extraction path, so no flag is needed.
2. Existing installs (development only) open on setup after updating. Their receipts database is untouched and appears after setup.
3. Rollback is reverting the PR. The downloaded model file is harmless and is deleted on uninstall.
