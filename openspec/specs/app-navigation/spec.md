# app-navigation Specification

## Purpose

Defines the app's top-level navigation shell: which top-level destinations exist, how the user moves between them on different window sizes, and how back navigation and system bars behave.

## Requirements

### Requirement: Top-level destinations
The app SHALL provide three top-level destinations, Receipts, Ask and Settings, each with an icon and a text label. The app SHALL open on Receipts.

#### Scenario: App launch
- **WHEN** the user launches the app
- **THEN** the Receipts destination is shown and marked as selected in the navigation UI

#### Scenario: Destinations listed
- **WHEN** the navigation UI is visible
- **THEN** it shows Receipts, Ask and Settings, in that order, each with an icon and a label

### Requirement: Switching destinations
Selecting a top-level destination SHALL show that destination and mark it as selected. Selecting the destination that is already selected SHALL NOT add another copy of it to the navigation history.

#### Scenario: Select another destination
- **WHEN** the user is on Receipts and selects Ask
- **THEN** the Ask destination is shown and Ask is marked as selected

#### Scenario: Reselect current destination
- **WHEN** the user is on Ask and selects Ask again
- **THEN** Ask stays shown, and a single back press from Ask still returns to Receipts

### Requirement: Adaptive navigation UI
The app SHALL show top-level navigation as a bottom navigation bar in compact-width windows and as a navigation rail in medium- and expanded-width windows.

#### Scenario: Phone in portrait
- **WHEN** the app runs in a compact-width window
- **THEN** top-level navigation is shown as a bottom navigation bar

#### Scenario: Tablet or unfolded foldable
- **WHEN** the app runs in a medium- or expanded-width window
- **THEN** top-level navigation is shown as a navigation rail

#### Scenario: Window resized
- **WHEN** the window width changes between compact and medium while the app is open
- **THEN** the navigation UI switches form and the selected destination stays the same

### Requirement: Back navigation between top-level destinations
Back from any top-level destination other than Receipts SHALL return to Receipts. Back from Receipts SHALL leave the app. The app SHALL support predictive back.

#### Scenario: Back from Ask or Settings
- **WHEN** the user is on Ask or Settings and presses or swipes back
- **THEN** Receipts is shown and marked as selected

#### Scenario: Back from Receipts
- **WHEN** the user is on Receipts and presses or swipes back
- **THEN** the app closes to the previous screen or launcher

#### Scenario: Predictive back preview
- **WHEN** the user starts a back swipe from Ask or Settings
- **THEN** the system shows a preview of the destination back will lead to

### Requirement: Navigation state survives configuration changes
The selected top-level destination SHALL survive configuration changes and process recreation.

#### Scenario: Device rotated
- **WHEN** the user is on Settings and rotates the device
- **THEN** Settings is still shown and selected after rotation

#### Scenario: Process recreated
- **WHEN** the system kills the app in the background while Ask is shown and the user returns to it
- **THEN** Ask is shown and selected

### Requirement: Edge-to-edge layout
The app SHALL draw edge-to-edge behind the system bars, and no interactive element or text SHALL be hidden behind the status bar, navigation bar or display cutout.

#### Scenario: Gesture navigation
- **WHEN** the device uses gesture navigation
- **THEN** the bottom navigation bar sits above the gesture area and all its items can be tapped

#### Scenario: Three-button navigation
- **WHEN** the device uses three-button navigation
- **THEN** no navigation item or content is covered by the system navigation bar

### Requirement: Placeholder destination content
Until their features are delivered, each top-level destination SHALL show a screen headed with the destination's name.

#### Scenario: Placeholder shown
- **WHEN** the user opens Ask before the Ask feature exists
- **THEN** a screen headed "Ask" is shown
