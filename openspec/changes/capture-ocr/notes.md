# Change Notes

## Task 1.2: device spike (2026-11-16, emulator Pixel_10, Google Play image, arm64)

- **Excluding `transport-backend-cct` breaks ML Kit.** `GmsDocumentScanning.getClient` and `TextRecognition.getClient` both crash with `NoClassDefFoundError: com.google.android.datatransport.cct.CCTDestination`. D8 was revised with the owner: keep the classes, remove the backend registration and INTERNET in the app manifest.
- **With the backend registration removed**, OCR and scanning work. `transport-runtime` logs `BackendRegistry: Could not retrieve metadata, returning empty list of transport backends` and `TransportRuntime: Transport backend 'cct' is not registered`, so events are dropped. No upload attempt appears in logcat. The merged manifest has no INTERNET; only `ACCESS_NETWORK_STATE` comes from the transport libraries.
- **OCR on a drawn receipt** (`CORNER SHOP`, items, `SUBTOTAL 200.00`, `TAX 10.00`, `TOTAL 210.00`): 13 lines. Every label and its amount came back as separate lines (for example `TOTAL` at x 80 and `210.00` at x 562, same y), which confirms that `OcrRowAssembler` is needed.
- **Scanner result**: `file:///data/user/0/com.bhavnathacker.pocketreceipts/cache/mlkit_docscan_ui_client/<n>.jpg`, inside `cacheDir`. After deleting it, `run-as … ls -R cache files` shows only the empty `mlkit_docscan_ui_client` folder and no other copy.
- **Module not installed yet**: `getStartScanIntent` succeeds. The scanner activity shows "Downloading updates to Google Play services…" and then opens the camera. The app needs no extra state.
- **FULL vs BASE_WITH_FILTER download size**: not measured; the module downloaded once for FULL. FULL stays (D5).
- Not covered by the spike: importing from the gallery in the scanner UI (left to the manual checks in 8.2).

## Task 8.1: verification (2026-11-16, emulator Pixel_10)

- `./gradlew clean`, then `./gradlew :build-logic:convention:test check assembleDebug`: green. All privacy checks pass. The merged debug manifest has no INTERNET and no `backend:` registration; it keeps `ACCESS_NETWORK_STATE` from the transport libraries.
- JVM tests: 294 (debug variants) plus 27 build-logic tests, all passing.
- Connected tests, run one module at a time (running them in parallel hit the known emulator flake "0 of which were compatible" / `ShellCommandUnresponsiveException`): `:app` 34, `:data:receipts` 7, `:data:ocr` 6, `:feature:receipts` 49, `:feature:capture` 8. That is 104, all passing.

## Task 8.2: manual checks (2026-11-16, emulator Pixel_10, en-US, Google Play image)

- **Camera scan** (the emulator's virtual scene, a checkerboard with no text): the review form opens with the manual-entry defaults and shows the snackbar "Nothing could be read from this scan".
- **Gallery import** of a drawn receipt (CORNER SHOP / 03/02/2026 / … / SUBTOTAL 200.00 / TAX 10.00 / TOTAL 210.00): the review form is prefilled with CORNER SHOP, 210.00 USD and Mar 2, 2026 (month first in en-US). The total chips are $210.00 (selected), $200.00, $120.00, $80.00 and $10.00.
- **Bug found and fixed**: the last chip first read "TAX 10.00". On Android, `java.util.Currency.getInstance` accepts any three-letter code (it is backed by ICU), so `TAX` became a currency. Marker codes are now checked against `Currency.getAvailableCurrencies()`. The desktop JVM throws for unknown codes, so the existing unit tests passed before the fix and pass after it.
- **Chip and save**: tapping $200.00 and saving lists "CORNER SHOP · $200.00" under Receipts.
- **Cancel**: closing the scanner returns to the list, and no receipt is created. Back from the review form returns to the list, and nothing is saved.
- **Rotation**: after picking the $200.00 chip and rotating, the total is still 200.00 with that chip selected, and all candidates are kept. The landscape layout uses the navigation rail.
- **Image cleanup**: after every scan, `run-as … ls -R cache` shows only the empty `mlkit_docscan_ui_client` folder and no `.jpg`.
- **Airplane mode**: the scanner opens, and both a camera scan and a gallery scan are read (the gallery scan was prefilled correctly).
- **Receipts stays selected** in the navigation bar on the review form.
- **APK size**: ML Kit adds a native OCR library of 10.6 MB on arm64-v8a (11.1 MB x86_64, 11.0 MB x86, 6.5 MB armeabi-v7a), stored uncompressed, plus 1.4 MB of model assets. That is about 12 MB per ABI, three times the "about 4 MB" estimated in the proposal; the proposal and design are corrected. The universal debug APK is 54 MB.
