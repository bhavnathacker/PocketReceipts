## MODIFIED Requirements

### Requirement: Top-level destinations
The app SHALL provide two top-level destinations, Receipts and Ask, each with an icon and a text label. Once the on-device model is ready, the app SHALL open on Receipts. Until then it SHALL open on model setup, which shows no navigation UI.

#### Scenario: App launch
- **WHEN** the model is ready and the user launches the app
- **THEN** the Receipts destination is shown and marked as selected in the navigation UI

#### Scenario: App launch before setup
- **WHEN** the model isn't ready and the user launches the app
- **THEN** model setup is shown and no navigation bar or rail is visible

#### Scenario: Setup finishes
- **WHEN** model setup finishes while the app is open
- **THEN** the Receipts destination is shown with the navigation UI, and back from Receipts leaves the app

#### Scenario: Destinations listed
- **WHEN** the navigation UI is visible
- **THEN** it shows Receipts and Ask, in that order, each with an icon and a label, and no Settings destination

### Requirement: Navigation state survives configuration changes
The selected top-level destination SHALL survive configuration changes and process recreation.

#### Scenario: Device rotated
- **WHEN** the user is on Ask and rotates the device
- **THEN** Ask is still shown and selected after rotation

#### Scenario: Process recreated
- **WHEN** the system kills the app in the background while Ask is shown and the user returns to it
- **THEN** Ask is shown and selected

## ADDED Requirements

### Requirement: Back navigation to Receipts
Back from Ask SHALL return to Receipts. Back from Receipts SHALL leave the app. The app SHALL support predictive back.

#### Scenario: Back from Ask
- **WHEN** the user is on Ask and presses or swipes back
- **THEN** Receipts is shown and marked as selected

#### Scenario: Back from Receipts
- **WHEN** the user is on Receipts and presses or swipes back
- **THEN** the app closes to the previous screen or launcher

#### Scenario: Predictive back preview
- **WHEN** the user starts a back swipe from Ask
- **THEN** the system shows a preview of the destination back will lead to

## REMOVED Requirements

### Requirement: Placeholder destination content
**Reason**: Settings, the last placeholder destination, is removed; Receipts and Ask both show their features.
**Migration**: None. Ask's content is specified by `spending-questions`, and Receipts' content by `receipt-management`.

### Requirement: Back navigation between top-level destinations
**Reason**: Its scenarios named Settings, which is removed; it is replaced by "Back navigation to Receipts" with the same behaviour for Ask.
**Migration**: None. The behaviour for Receipts and Ask is unchanged.
