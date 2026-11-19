# model-setup Specification

## Purpose

Gets the on-device Gemma model that reads receipts onto the phone, verifies it, and keeps the app closed until the model is ready, because the app has no way to read receipts without it.

## Requirements

### Requirement: App gated on the model
Until the Gemma model is on the phone, verified and loadable, the app SHALL show only the model setup screen. The Receipts list, manual entry, scanning, Ask and Settings SHALL NOT be reachable from it. Once the model is ready, the app SHALL open normally on every later launch without showing setup again.

#### Scenario: First launch
- **WHEN** the user opens the app for the first time
- **THEN** the model setup screen is shown, with no navigation bar or rail

#### Scenario: Model already set up
- **WHEN** the model was downloaded and verified on an earlier launch and the user opens the app
- **THEN** the Receipts list is shown, without the setup screen

#### Scenario: Back from setup
- **WHEN** the setup screen is shown and the user presses back
- **THEN** the app closes

### Requirement: Download the model
The setup screen SHALL state the download size (2.6 GB) before anything is downloaded. It SHALL start the download only when the user taps Download, and show progress while downloading. The download SHALL continue while the app is in the background or closed, and setup SHALL show its current progress when the app is reopened.

#### Scenario: Start the download
- **WHEN** the user taps Download on the setup screen
- **THEN** the download starts and the screen shows the percentage downloaded

#### Scenario: App closed during download
- **WHEN** the download is in progress, the user closes the app and reopens it later
- **THEN** setup shows the download's current progress, or opens the Receipts list if it finished and was verified

### Requirement: Verify the downloaded model
The app SHALL use a downloaded model file only when its size and SHA-256 checksum match the expected Gemma file. A file that doesn't match SHALL be deleted, and the setup screen SHALL report the failure and offer Retry.

#### Scenario: Corrupt download
- **WHEN** the downloaded file's checksum doesn't match
- **THEN** the file is deleted, and setup says the download failed and shows Retry

#### Scenario: Verified download
- **WHEN** the downloaded file's size and checksum match
- **THEN** the model is loaded and the Receipts list is shown

### Requirement: Download failures
When the download can't start or fails, setup SHALL say why in plain words (no internet connection, not enough storage, or download failed) and offer Retry. Retry SHALL start the download again. The app SHALL NOT offer any way to use it without the model.

#### Scenario: No connection
- **WHEN** the user taps Download with no network connection and the download fails
- **THEN** setup says there is no internet connection and shows Retry

#### Scenario: Not enough storage
- **WHEN** the phone has less free storage than the model needs and the user taps Download
- **THEN** setup says how much space is needed, and no download starts

#### Scenario: Retry
- **WHEN** a download has failed and the user taps Retry
- **THEN** the download starts again from the setup screen

### Requirement: Phone can't run the model
When a verified model can't be loaded on the phone (for example, not enough memory), setup SHALL say that this phone can't run the receipt reader, and SHALL offer Retry. The app SHALL stay on the setup screen.

#### Scenario: Model fails to load
- **WHEN** the model file is verified but loading it fails
- **THEN** setup says this phone can't run the receipt reader and shows Retry

### Requirement: Model stored privately
The model file SHALL be stored in the app's own storage, SHALL NOT be included in cloud backup or device transfer, and SHALL be removed when the app is uninstalled. The model download SHALL be the only network request the app makes, and it SHALL NOT send any receipt data.

#### Scenario: Backup
- **WHEN** the phone backs up the app's data
- **THEN** the model file is not in the backup

#### Scenario: Only the model is downloaded
- **WHEN** the app is used after setup, including scanning receipts
- **THEN** it makes no network requests
