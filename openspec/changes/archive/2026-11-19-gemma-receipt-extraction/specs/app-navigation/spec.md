# Spec Delta

## MODIFIED Requirements

### Requirement: Top-level destinations
The app SHALL provide three top-level destinations, Receipts, Ask and Settings, each with an icon and a text label. Once the on-device model is ready, the app SHALL open on Receipts. Until then it SHALL open on model setup, which shows no navigation UI.

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
- **THEN** it shows Receipts, Ask and Settings, in that order, each with an icon and a label
