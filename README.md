# PocketReceipts

[![CI](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml)

An Android app for keeping your receipts in your pocket.

> 🚧 Early development. You can scan receipts or add them by hand, and view, edit and delete them. On-device AI and Ask are next.

## Features

- **Scan receipts**: tap **Scan receipt** to photograph a paper receipt (or pick one from your gallery). Its text is read on your phone, and the review form opens prefilled with the merchant, date and total it found; tap a chip to switch to another date or total it found, then save. Scanning works offline once the scanner is installed, and needs Google Play services: Play services installs the scanner on first use. Without it, you can still add receipts by hand. The scanned image is deleted as soon as it has been read, and is never stored with the receipt.
- **Receipts**: a list of your receipts, newest first. Add one by hand with merchant, date, total, currency and category, plus an optional note. Open a receipt to edit or delete it.
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
   +------------+------------+------------+------------------+
   v            v            v            v                  v
 :feature:receipts  :feature:capture  :feature:ask  :feature:modelsetup  :feature:settings
   \______________________|____________________/
                          v
                      :domain   (pure Kotlin: models, use cases, interfaces)
                          ^
   +----------------------+----------------------+
 :data:receipts   :data:ocr   :data:ai   :data:model-download (only module with network access)

 shared: :core:ui (theme, components, icons)  :core:test  :core:ui-test
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
| INTERNET permission confinement | `:app:verifyInternetPermissionSources` | Only `:data:model-download` may add `android.permission.INTERNET`: not `:app`, not another module, not a third-party library. The one exception is ML Kit's usage-log uploader, whose permission is accepted only because the app manifest removes it. |
| No usage-log uploads | `:app:verifyNoUsageLoggingBackend` | No usage-logging upload backend may be registered in the merged manifest. ML Kit (scanning and text recognition) needs Google's `transport-backend-cct` classes, so the library stays, but `app/src/main/AndroidManifest.xml` removes its backend registration and INTERNET permission. ML Kit's usage events are then dropped on the phone. |

Run them all with:

```bash
./gradlew check
```

Receipt scans are read on the phone by ML Kit's bundled text recognizer, and the scanned image is deleted right after it has been read.

Receipts are also kept out of backups: the receipts database is excluded from cloud backup and device-to-device transfer (`app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`), so receipts stay on this phone.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
