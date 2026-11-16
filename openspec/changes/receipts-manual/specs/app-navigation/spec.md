# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Placeholder destination content
Until their features are delivered, the Ask and Settings destinations SHALL each show a screen headed with the destination's name. The Receipts destination SHALL show the user's receipts.

#### Scenario: Placeholder shown
- **WHEN** the user opens Ask before the Ask feature exists
- **THEN** a screen headed "Ask" is shown

#### Scenario: Receipts is not a placeholder
- **WHEN** the user opens Receipts
- **THEN** the receipt list (or its empty state) is shown
