# Proposal

## Why

The hand-written rules that pick the merchant, date, total and category from a scan's text (`MerchantCandidateParser`, `DateCandidateParser`, `AmountCandidateParser`, `CategoryCandidateParser`) keep failing on real receipts. They pick an address as the shop name, take a licence number as the total, and miss `31-0ct-26`. Each fix only covers the receipts we have already seen. A language model reading the same text understands which line is the shop, the invoice date and the amount paid. Running Gemma on the phone keeps the app's promise that receipt data never leaves the device.

## What Changes

- **New: on-device Gemma.** The app uses Gemma 4 E2B (`gemma-4-E2B-it.litertlm`, 2.6 GB, Apache 2.0, from Hugging Face `litert-community`). It is downloaded once through `:data:model-download` and run with LiteRT-LM in `:data:ai`. Gemma is the only model: no Gemini Nano.
- **New: model setup screen (`:feature:modelsetup`).**
  - Until Gemma is downloaded, verified and loads, the app shows only this screen. It gives the download size, a Download button, progress, and Retry on failure.
  - **BREAKING:** the Receipts list, manual entry and scanning are not reachable without the model. If Gemma can't be downloaded or can't run on the phone, the app doesn't work.
- **Changed: scan extraction.**
  - The ML Kit Document Scanner and ML Kit text recognition stay.
  - The recognized text goes to Gemma, which returns one merchant, purchase date, total, currency and category as JSON.
  - The app checks the answer before prefilling the review form. A future date, an unknown currency or category, or an amount it can't parse is dropped.
- **BREAKING: rule-based parsers removed.** `MerchantCandidateParser`, `DateCandidateParser`, `AmountCandidateParser`, `CategoryCandidateParser` and `ReceiptScanParser` are deleted, along with their tests. There is no fallback to them.
- **Changed: review form.**
  - It is prefilled with Gemma's single value for each field.
  - The date and total candidate chips are removed. The user edits fields directly, then saves as before.
- **Changed: network and permission rules.**
  - `:data:model-download` declares `INTERNET`, and the download uses Android's `DownloadManager`. No HTTP client library is added.
  - The app manifest stops removing `INTERNET`, because the removal would also strip the model download's permission.
  - ML Kit's usage logging stays switched off through the existing backend meta-data removal, still enforced by `verifyNoUsageLoggingBackend`.
  - The `INTERNET` source check accepts ML Kit's data-transport library only while that backend stays unregistered.

## Capabilities

### New Capabilities
- `model-setup`: downloading, verifying and loading the on-device Gemma model, and gating the app on it.

### Modified Capabilities
- `receipt-capture`:
  - The Date candidates, Total candidates, Currency from the receipt, Merchant candidate and Category from the shop's name requirements are replaced by one "Receipt fields read by Gemma" requirement.
  - Review before saving and Review survives configuration changes lose the candidate chips.
  - Reading fails also covers Gemma failing.
- `app-navigation`: the app opens on model setup until the model is ready, and on Receipts after that.
- `privacy-guarantees`: the Internet permission requirement changes. The model-download module now declares INTERNET, and ML Kit's data-transport library is accepted because its upload backend is unregistered, not because the app removes INTERNET.

## Impact

- **Modules:**
  - `:data:model-download`: `DownloadManager` download, size and SHA-256 check, `INTERNET` permission.
  - `:data:ai`: LiteRT-LM engine, prompt, JSON parsing.
  - `:feature:modelsetup`: setup screen and ViewModel.
  - `:domain`: new repository interfaces and use cases. The `scan` parsers are removed and `ReceiptScan` is simplified.
  - `:feature:receipts`: form state, `ScanPrefill`, mappers and chips.
  - `:feature:capture`: reading progress copy.
  - `:app`: start gate, manifest, Hilt wiring.
- **Dependencies:** `com.google.ai.edge.litertlm:litertlm-android` (version pinned in `libs.versions.toml`). Hilt is already at 2.60.1, which LiteRT-LM needs.
- **Build:** `PrivacyRules` and its tests (INTERNET acceptance rule), and `verifyInternetPermissionSources`.
- **Device requirements:**
  - 2.6 GB of free storage, a one-time 2.6 GB download, and about 1.7 GB of RAM while reading a receipt.
  - Reading takes a few seconds longer than the rule-based parsers.
  - Phones that can't load the model can't use the app.
- **Docs:** README (features, privacy guarantees table, module diagram) and `CLAUDE.md` (the ML Kit / INTERNET manifest note).
