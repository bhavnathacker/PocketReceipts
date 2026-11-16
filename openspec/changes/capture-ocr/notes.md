# Change Notes

## Task 1.2: device spike (2026-11-16, emulator Pixel_10, Google Play image, arm64)

- **Excluding `transport-backend-cct` breaks ML Kit.** `GmsDocumentScanning.getClient` and `TextRecognition.getClient` both crash with `NoClassDefFoundError: com.google.android.datatransport.cct.CCTDestination`. D8 was revised with the owner: keep the classes, remove the backend registration and INTERNET in the app manifest.
- **With the backend registration removed**, OCR and scanning work. `transport-runtime` logs `BackendRegistry: Could not retrieve metadata, returning empty list of transport backends` and `TransportRuntime: Transport backend 'cct' is not registered`, so events are dropped. No upload attempt appears in logcat. The merged manifest has no INTERNET; only `ACCESS_NETWORK_STATE` comes from the transport libraries.
- **OCR on a drawn receipt** (`CORNER SHOP`, items, `SUBTOTAL 200.00`, `TAX 10.00`, `TOTAL 210.00`): 13 lines. Every label and its amount came back as separate lines (for example `TOTAL` at x 80 and `210.00` at x 562, same y), which confirms that `OcrRowAssembler` is needed.
- **Scanner result**: `file:///data/user/0/com.bhavnathacker.pocketreceipts/cache/mlkit_docscan_ui_client/<n>.jpg`, inside `cacheDir`. After deleting it, `run-as … ls -R cache files` shows only the empty `mlkit_docscan_ui_client` folder and no other copy.
- **Module not installed yet**: `getStartScanIntent` succeeds. The scanner activity shows "Downloading updates to Google Play services…" and then opens the camera. The app needs no extra state.
- **FULL vs BASE_WITH_FILTER download size**: not measured; the module downloaded once for FULL. FULL stays (D5).
- Not covered by the spike: importing from the gallery in the scanner UI (left to the manual checks in 8.2).
