# Proposal

## Why

Typing every receipt by hand is the slowest part of the app, and scanning is the app's core promise. Receipts can now be stored and edited (`receipts-manual`). The next step is filling the receipt form from a photo: scan the paper receipt, read its text on the device, and prefill the date, total and merchant for the user to confirm. This works without the AI model, so scanning is useful before `model-setup` and `ai-extraction` land. Those changes will later improve which candidates are picked.

## What Changes

- **Scan entry point**: the Receipts list offers "Scan receipt" next to "Add receipt", in the list and in the empty state.
- **Document scanning** in `:feature:capture`: the ML Kit Document Scanner (Google Play services UI) captures one page, with edge detection, cropping and import from the gallery. When the scanner isn't available (no Google Play services, or its module can't be installed), the user is offered manual entry instead.
- **On-device text recognition** in a new `:data:ocr` module: ML Kit Text Recognition (Latin script, model bundled in the APK) reads the scanned image into text lines. The image is deleted as soon as it has been read; it is never stored with the receipt.
- **Candidate parsers** in `:domain` (pure Kotlin): find date candidates, total-amount candidates (with currency when printed) and a merchant candidate in the recognised lines, and preselect the most likely date and total.
- **Review form**: the existing receipt form opens prefilled from the scan, titled "Review receipt". Under the date and total fields it shows the other candidates as chips the user can tap to switch. Nothing is saved until the user taps Save.
- **ML Kit usage logging switched off**: ML Kit pulls in Google's `transport-backend-cct` usage-logging backend, which adds the INTERNET permission. ML Kit can't run without its classes (a device spike showed both the scanner and text recognition crash when it is excluded), so the library stays, but the app manifest removes its backend registration and its INTERNET permission. With no backend registered, ML Kit's usage events are dropped on the device. A new build check fails if any upload backend is registered in the merged manifest, and the INTERNET check accepts the backend's permission only because the app removes it.

## Capabilities

### New Capabilities
- `receipt-capture`: scanning a paper receipt, reading its text on the device, extracting date, total and merchant candidates, and confirming them on a review form before saving.

### Modified Capabilities
- `receipt-management`: the empty receipt list offers scanning as well as manual entry.
- `privacy-guarantees`: the build also fails when a third-party usage-logging upload backend is registered in the merged manifest (ML Kit's `transport-backend-cct`), and the INTERNET check accepts that backend's permission only when the app manifest removes it.

## Impact

- **Modules**: `:domain` (scan models, `TextRecognitionRepository`, candidate parsers, `ScanReceiptUseCase`), new `:data:ocr` (ML Kit text recognition, Hilt bindings), `:feature:capture` (scanner launch, reading progress, error states, `CaptureKey`), `:feature:receipts` (scan action, review mode of the form with candidate chips), `:app` (navigation wiring, `:data:ocr` dependency, manifest removals), `build-logic` (usage-logging backend check, INTERNET check exception).
- **Dependencies**: `com.google.android.gms:play-services-mlkit-document-scanner` 16.0.0 and `com.google.mlkit:text-recognition` 16.0.1, with `com.google.android.datatransport:transport-backend-cct` kept on the classpath but switched off in the app manifest. Neither is an HTTP client.
- **Google Play services**: scanning needs Google Play services. On first use, Play services installs the scanner module itself, using its own network access rather than the app's. The scanner runs on the device, and the app receives the image as a file. On devices without Play services, scanning is unavailable and manual entry still works.
- **APK size**: the bundled Latin text-recognition model adds about 12 MB per ABI (measured in task 8.2: a 10.6 MB native library on arm64 plus 1.4 MB of model assets; the first estimate was 4 MB). That is accepted so OCR works offline from the first launch. ABI splits (or an App Bundle) come in release-readiness.
- **Out of scope**: keeping or showing the receipt image, multi-page receipts, non-Latin scripts, line items, AI-picked fields (ai-extraction), saving unfinished scans as drafts, and batch scanning.
