# PocketReceipts

[![CI](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml)

An Android app for keeping your receipts in your pocket.

> 🚧 Early development. You can scan receipts or add them by hand, and view, edit and delete them. Receipts are read by Gemma, an AI model that runs on your phone, and you can ask how much you spent. The app has two tabs: Receipts and Ask.

## Features

- **Receipt reader setup**: on first launch the app downloads Gemma 4 E2B (2.6 GB, once), an AI model that runs on your phone, and checks the download before using it. The app can't be used until it is set up: the setup screen shows the download's progress and offers Retry when there's no connection, not enough storage, or the download fails. Phones that can't run the model (typically under about 6 GB of RAM) can't use the app.
- **Scan receipts**: tap **Scan receipt** to photograph a paper receipt (or pick one from your gallery). ML Kit reads its text on your phone, Gemma reads the merchant, date, total (the amount actually paid, such as the net amount, rather than a gross total), currency and category from that text, and the review form opens prefilled with them. Edit anything that's wrong, then save. Reading takes a few seconds and works offline. Gemma starts loading as soon as you open the scanner, so reading can start the moment you finish; it then stays loaded for 2 minutes in case you scan another receipt, and is unloaded sooner if you leave the app. Scanning needs Google Play services, which installs the scanner on first use; without it, you can still add receipts by hand. The scanned image is deleted as soon as its text has been read, and is never stored with the receipt.
- **Receipts**: a list of your receipts, newest first. Add one by hand with merchant, date, total, currency and category, plus an optional note. Open a receipt to edit or delete it.
- **Ask about your spending**: type a question such as "How much did I spend on groceries in October?", "How much is my total spend in 2025?" or "How much did I spend on shopping this quarter?", or tap one of the examples. Gemma reads only the category and the period from your question (a month, a quarter, a year, a date range or all time); the app then adds up the matching receipts exactly, on your phone. The answer shows the category and dates it used, the total and the number of receipts, with one total per currency (amounts are never converted). Questions about anything else, such as shops, averages or the biggest purchase, get a "can't answer" message. Gemma starts loading when you open Ask, so it's ready by the time you've typed. Questions and answers aren't stored.
- **Exact amounts in any currency**: each receipt keeps its own ISO 4217 currency (new ones default to your phone's). Totals are stored as whole minor units (cents, paise...), so nothing is rounded. Amounts are entered and shown in your locale's format, for example `₹1,23,456.50` in India.

## Tech stack

- **Kotlin** with **Jetpack Compose** and **Material 3**
- Android Gradle Plugin 9, Gradle version catalog (`gradle/libs.versions.toml`)
- Min SDK 26 · Target SDK 37

## Getting started

### Requirements

- A recent version of [Android Studio](https://developer.android.com/studio)
- Android SDK 37 (Android Studio will prompt you to install it)

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
./gradlew test               # run unit tests
```

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

The model download is the only network request the app makes, and it sends no receipt data. Receipt scans are read on the phone: ML Kit's bundled text recognizer reads the text, and Gemma, run with LiteRT-LM, reads the receipt's fields from it. The scanned image is deleted right after its text has been read.

Receipts are also kept out of backups: the receipts database is excluded from cloud backup and device-to-device transfer (`app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`), so receipts stay on this phone. The model file (app-specific external storage, `models/`) is excluded too, and is removed when the app is uninstalled.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
