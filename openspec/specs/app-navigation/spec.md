# app-navigation Specification

## Purpose

Defines the app's top-level navigation shell: which top-level destinations exist, how the user moves between them on different window sizes, and how back navigation and system bars behave.

## Requirements

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

### Requirement: Navigation state survives configuration changes
The selected top-level destination SHALL survive configuration changes and process recreation.

#### Scenario: Device rotated
- **WHEN** the user is on Ask and rotates the device
- **THEN** Ask is still shown and selected after rotation

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

### Requirement: Screens within a top-level destination
A destination MAY open further screens (for example a receipt's detail or form) on top of itself. While such a screen is shown, its top-level destination SHALL stay selected. Back SHALL return to the screen beneath it. Selecting a different top-level destination SHALL leave the nested screens.

#### Scenario: Receipts stays selected on detail
- **WHEN** the user opens a receipt's detail screen from Receipts
- **THEN** Receipts is still marked as selected in the navigation UI

#### Scenario: Switching tabs from a nested screen
- **WHEN** the user is on a receipt's detail screen and selects Ask, then goes back
- **THEN** the Receipts destination is shown at its receipt list, not at the detail screen

#### Scenario: Nested screens survive rotation
- **WHEN** the user is on a receipt's edit form and rotates the device
- **THEN** the edit form for the same receipt is still shown
