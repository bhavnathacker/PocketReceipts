# PocketReceipts

An Android app for keeping your receipts in your pocket.

> 🚧 Early development — the project currently contains the starter app scaffold.

## Tech stack

- **Kotlin** with **Jetpack Compose** and **Material 3**
- Android Gradle Plugin 9, Gradle version catalog (`gradle/libs.versions.toml`)
- Min SDK 24 · Target SDK 37

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

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
