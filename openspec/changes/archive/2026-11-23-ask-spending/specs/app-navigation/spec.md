## MODIFIED Requirements

### Requirement: Placeholder destination content
Until its feature is delivered, the Settings destination SHALL show a screen headed with the destination's name. The Receipts destination SHALL show the user's receipts, and the Ask destination SHALL show spending questions.

#### Scenario: Placeholder shown
- **WHEN** the user opens Settings before the Settings feature exists
- **THEN** a screen headed "Settings" is shown

#### Scenario: Receipts is not a placeholder
- **WHEN** the user opens Receipts
- **THEN** the receipt list (or its empty state) is shown

#### Scenario: Ask is not a placeholder
- **WHEN** the user opens Ask
- **THEN** the question field and example questions are shown
