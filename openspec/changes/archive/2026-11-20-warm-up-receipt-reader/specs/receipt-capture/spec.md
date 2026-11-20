# Spec Delta

## ADDED Requirements

### Requirement: Receipt reader prepared while scanning
When the user starts a scan, the app SHALL start loading the on-device model, so that it loads while the user is still in the scanner. The model SHALL stay loaded while the scan and its reading are in progress, including while the scanner is in front of the app. Preparing the model SHALL NOT change what the user sees: if it fails, scanning and reading behave as without it.

#### Scenario: Model loaded while photographing
- **WHEN** the user taps "Scan receipt" and spends longer in the scanner than the model takes to load
- **THEN** reading starts on the loaded model as soon as the user finishes the scan, without loading it again

#### Scenario: Done before the model is loaded
- **WHEN** the user finishes the scan before the model has loaded
- **THEN** reading waits for the load already in progress and doesn't start a second one

#### Scenario: Scanner in front
- **WHEN** the scanner is open on top of the app for longer than 2 minutes
- **THEN** the model stays loaded

#### Scenario: Preparing fails
- **WHEN** loading the model fails while the user is in the scanner
- **THEN** the scanner works as usual, and reading the scan loads the model itself and, if that also fails, offers to try again or enter the receipt manually

### Requirement: Receipt reader kept briefly, then released
After a scan has been read or cancelled, the app SHALL keep the model loaded for 2 minutes, so another scan doesn't load it again. The app SHALL release the model's memory once 2 minutes pass with no scan in progress, or as soon as the app goes to the background with no scan in progress.

#### Scenario: Second scan soon after
- **WHEN** a scanned receipt has been read and the user starts another scan within 2 minutes
- **THEN** the second receipt is read without loading the model again

#### Scenario: Idle after scanning
- **WHEN** 2 minutes pass after a scan was read and no new scan was started, including while the review form is open
- **THEN** the model's memory is released

#### Scenario: App sent to the background
- **WHEN** no scan is in progress (for example, the review form or the Receipts list is shown) and the user leaves the app
- **THEN** the model's memory is released

#### Scenario: Scan cancelled
- **WHEN** the user closes the scanner without taking a scan
- **THEN** the Receipts list is shown, and the model is released 2 minutes later unless another scan starts

### Requirement: Receipts read independently
Each scanned receipt SHALL be read on its own, even when the model stays loaded between scans: nothing from an earlier receipt SHALL influence the values read from a later one.

#### Scenario: Two receipts in a row
- **WHEN** two different receipts are scanned one after the other on the same loaded model
- **THEN** the second receipt's values come only from its own text
