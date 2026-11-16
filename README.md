# PocketReceipts

[![CI](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/bhavnathacker/PocketReceipts/actions/workflows/ci.yml)

An Android app for keeping your receipts in your pocket.

> 🚧 Early development. You can add, view, edit and delete receipts by hand. Receipt scanning, on-device AI and Ask are next.

## Features

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
 :data:receipts       :data:ai          :data:model-download (only module with network access)

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
| INTERNET permission confinement | `:app:verifyInternetPermissionSources` | Only `:data:model-download` may add `android.permission.INTERNET`: not `:app`, not another module, not a third-party library. |

Run them all with:

```bash
./gradlew check
```

Receipts are also kept out of backups: the receipts database is excluded from cloud backup and device-to-device transfer (`app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`), so receipts stay on this phone.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
