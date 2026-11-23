# PocketReceipts

[![CI](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml)

An Android app for keeping your receipts in your pocket.

Scan a paper receipt or add one by hand, then ask how much you spent, all on your phone. Receipts are read and questions are answered by **Gemma 4 E2B**, an AI model that runs **on the device** with LiteRT-LM. No receipt, photo or question ever leaves the phone.

The app has two tabs: **Receipts** and **Ask**.

## Features

- **Receipt reader setup**: on first launch the app downloads Gemma 4 E2B (2.6 GB, once), an AI model that runs on your phone, and checks the download before using it. The app can't be used until it is set up: the setup screen shows the download's progress and offers Retry when there's no connection, not enough storage, or the download fails. Phones that can't run the model (typically under about 6 GB of RAM) can't use the app.
- **Scan receipts**: tap **Scan receipt** to photograph a paper receipt (or pick one from your gallery). ML Kit reads its text on your phone, Gemma reads the merchant, date, total (the amount actually paid, such as the net amount, rather than a gross total), currency and category from that text, and the review form opens prefilled with them. Edit anything that's wrong, then save. Reading takes a few seconds and works offline. Gemma starts loading as soon as you open the scanner, so reading can start the moment you finish; it then stays loaded for 2 minutes in case you scan another receipt, and is unloaded sooner if you leave the app. Scanning needs Google Play services, which installs the scanner on first use; without it, you can still add receipts by hand. The scanned image is deleted as soon as its text has been read, and is never stored with the receipt.
- **Receipts**: a list of your receipts, newest first. Add one by hand with merchant, date, total, currency and category, plus an optional note. Open a receipt to edit or delete it.
- **Ask about your spending**: type a question such as "How much did I spend on groceries in October?", "How much is my total spend in 2025?" or "How much did I spend on shopping this quarter?", or tap one of the examples. Gemma reads only the category and the period from your question (a month, a quarter, a year, a date range or all time); the app then adds up the matching receipts exactly, on your phone. The answer shows the category and dates it used, the total and the number of receipts, with one total per currency (amounts are never converted). Questions about anything else, such as shops, averages or the biggest purchase, get a "can't answer" message. Gemma starts loading when you open Ask, so it's ready by the time you've typed. Questions and answers aren't stored.
- **Exact amounts in any currency**: each receipt keeps its own ISO 4217 currency (new ones default to your phone's). Totals are stored as whole minor units (cents, paise...), so nothing is rounded. Amounts are entered and shown in your locale's format, for example `₹1,23,456.50` in India.

## How it works

**Scanning a receipt**

```
ML Kit document scanner ─► ML Kit text recognition ─► OcrRowAssembler ─► Gemma (LiteRT-LM)
   (photo, crop)             (on-device, bundled)      (rebuilds rows)    (JSON: merchant, date,
                                                                            total, currency, category)
        │                                                                           │
        └── image deleted as soon as its text is read          ReceiptFieldsChecker ◄┘
                                                               (drops invalid values)
                                                                           │
                                                             review form, prefilled ─► Room
```

**Asking a question**

```
"How much did I spend on groceries in October?"
        │
        ▼
Gemma ─► {"category": "GROCERIES", "period": {"type": "month", "year": null, "month": 10}}
        │
        ▼
SpendingQueryChecker ─► Groceries, 2026-10-01 … 2026-10-31   (rejects anything it can't check)
        │
        ▼
SQL: SUM(total_minor) GROUP BY currency ─► "Groceries · 1 – 31 Oct 2026 · ₹2,450.00 · 7 receipts"
```

Gemma only turns text into structured data. Every value it returns is checked in the domain layer before it is used, and it never does arithmetic: totals are exact integer sums done by the database.

**Running the model**

- `LiteRtModelRuntime` owns one shared engine, which scanning and Ask both use, with all inference on a single dedicated thread.
- The engine runs on the GPU where OpenCL is available (on a Snapdragon 8+ Gen 1 phone, a warm read took about 5.5 s instead of 13 s) and on the CPU otherwise. If the GPU fails to load the model or to read, the runtime switches to the CPU for the rest of the session.
- The model is loaded ahead of use, while the scanner or Ask is open. It is released 2 minutes after its last use, or at once when the app goes to the background, to give its memory (about 1 GB) back.

## Design decisions

- **On-device AI only.** Receipts are personal financial data, so the app runs Gemma on the phone and the build forbids network clients outside the model download (see [Privacy guarantees](#privacy-guarantees)).
- **The model's output is never trusted.** Its replies are parsed leniently and then validated (`ReceiptFieldsChecker`, `SpendingQueryChecker`). An invalid value is dropped and the field is left at its default; nothing is guessed. The receipt form always lets the user correct the values before saving.
- **Exact money.** Amounts are `Long` minor units with an ISO 4217 code (`Money`). They are never floating point, and are formatted with ICU for the user's locale, including Indian lakh/crore grouping.
- **Clean architecture with enforced boundaries.** Features and data modules depend only on `:domain`; `verifyModuleDependencies` fails the build otherwise.
- **Spec-driven development.** Behaviour is specified in [`openspec/specs`](openspec/specs) with testable scenarios. Every change went through a proposal, design and task list ([`openspec/changes/archive`](openspec/changes/archive)) before implementation, and was reviewed as a pull request.

## Known limitations

- The model needs a phone with roughly 6 GB of RAM or more, and a one-time 2.6 GB download.
- Ask answers totals by category and period only. There are no questions about merchants, averages or comparisons, and no currency conversion.
- Reading quality depends on the photo and on Gemma. The review form is there so the user can fix values before saving.
- The UI is in English only.
- R8 optimization is switched off for release builds (`app/build.gradle.kts`): keep rules for LiteRT-LM and ML Kit haven't been written and tested yet.

## Tech stack

| Area | Libraries |
|---|---|
| Language and UI | Kotlin 2.4, Jetpack Compose (Material 3, adaptive navigation suite), edge-to-edge |
| Architecture | Clean architecture, multi-module, MVVM with `StateFlow`, Hilt (with assisted injection), Navigation 3 |
| Data | Room (exported schemas), kotlinx.serialization, Coroutines and Flow |
| On-device AI | Gemma 4 E2B via LiteRT-LM 0.19 (GPU/OpenCL or CPU), ML Kit Document Scanner and Text Recognition |
| Model delivery | Android `DownloadManager`, SHA-256 verification |
| Testing | JUnit 5, Mockito-Kotlin, Turbine, Truth, Compose UI tests, Hilt testing, in-memory Room |
| Build | AGP 9, convention plugins in `build-logic`, version catalog, configuration cache, Kover coverage, GitHub Actions CI |

Min SDK 26 · Target SDK 37.

## Getting started

### Requirements

- A recent version of [Android Studio](https://developer.android.com/studio)
- Android SDK 37 (Android Studio will prompt you to install it)
- To use the app: a phone or emulator with Google Play services (for the document scanner), about 3 GB of free storage, and roughly 6 GB of RAM or more for the model. The first launch downloads the model.

### Build and run

1. Clone the repository:
   ```bash
   git clone https://github.com/bhavnathacker/PocketReceipts.git
   ```
2. Open the project in Android Studio and let Gradle sync.
3. Select an emulator or connected device and click **Run**.

Or from the command line:

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # install on a connected device/emulator
```

## Testing

```bash
./gradlew check                    # unit tests, lint, module-dependency and privacy checks (what CI runs)
./gradlew -p build-logic test      # convention plugin and privacy rule tests
./gradlew connectedAndroidTest     # instrumented and Compose UI tests, needs a device or emulator
./gradlew koverHtmlReport          # coverage report in build/reports/kover
```

- **Unit tests (JVM)** cover use cases, checkers, ViewModels, mappers, prompt builders and reply parsers, the model runtime (with a fake engine factory), and the model download repository.
- **Instrumented tests** cover the Room DAO, ICU formatting (money, date ranges), every screen (Compose UI tests), and the app shell with Hilt, including navigation, model setup gating and configuration changes. The app tests replace the model download with `FakeModelRepository`, so they never download or load the 2.6 GB model.

## Modules

```
                 :app  (composition root: Hilt, navigation shell)
   +------------+------------+------------+
   v            v            v            v
 :feature:receipts  :feature:capture  :feature:ask  :feature:modelsetup
   \______________________|____________________/
                          v
                      :domain   (pure Kotlin: models, use cases, interfaces)
                          ^
   +----------------------+----------------------+
 :data:receipts   :data:ocr   :data:ai (Gemma via LiteRT-LM)   :data:model-download (only module with network access)

 shared: :core:ui (theme, icons, money and category formatting)  :core:test  :core:ui-test
```

Dependency rules, checked by `./gradlew check`:

- Features depend only on `:domain` and `:core:*`, never on data modules or other features.
- Data modules depend on `:domain`, never on features.
- `:app` wires everything together.

Build configuration for each module type lives in convention plugins in [`build-logic`](build-logic).

## Privacy guarantees

Pocket Receipts keeps your spending data on your phone. The build enforces this:
`./gradlew check` fails if any of these rules is broken.

| Check | Task | Rule |
|---|---|---|
| HTTP client confinement | `verifyNoHttpClient` (every module) | No HTTP client library (OkHttp, Retrofit, Ktor, ...) on any module's compile classpath, except `:data:model-download`, which downloads the on-device AI model. |
| No tracking SDKs | `:app:verifyNoTrackingSdks` | No analytics, advertising or crash-reporting SDK (Firebase Analytics/Crashlytics, Sentry, ...) anywhere in the app. |
| INTERNET permission confinement | `:app:verifyInternetPermissionSources` | Only `:data:model-download` may add `android.permission.INTERNET` (the model download uses Android's `DownloadManager`): not `:app`, not another module, not a third-party library. The one exception is ML Kit's usage-log uploader, whose permission is accepted only because the app manifest removes its upload backend's registration, so it has no way to send anything. |
| No usage-log uploads | `:app:verifyNoUsageLoggingBackend` | No usage-logging upload backend may be registered in the merged manifest. ML Kit (scanning and text recognition) needs Google's `transport-backend-cct` classes, so the library stays, but `app/src/main/AndroidManifest.xml` removes its backend registration. ML Kit's usage events are then dropped on the phone. |

Run them all with:

```bash
./gradlew check
```

The model download is the only network request the app makes, and it sends no receipt data. Receipt scans are read on the phone: ML Kit's bundled text recognizer reads the text, and Gemma, run with LiteRT-LM, reads the receipt's fields from it. The scanned image is deleted right after its text has been read. Ask's questions are read by the same on-device model, and are never stored or logged.

Receipts are also kept out of backups: the receipts database is excluded from cloud backup and device-to-device transfer (`app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`), so receipts stay on this phone. The model file (app-specific external storage, `models/`) is excluded too, and is removed when the app is uninstalled.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
