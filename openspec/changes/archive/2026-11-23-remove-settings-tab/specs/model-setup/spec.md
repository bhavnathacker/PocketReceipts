## MODIFIED Requirements

### Requirement: App gated on the model
Until the Gemma model is on the phone, verified and loadable, the app SHALL show only the model setup screen. The Receipts list, manual entry, scanning and Ask SHALL NOT be reachable from it. Once the model is ready, the app SHALL open normally on every later launch without showing setup again.

#### Scenario: First launch
- **WHEN** the user opens the app for the first time
- **THEN** the model setup screen is shown, with no navigation bar or rail

#### Scenario: Model already set up
- **WHEN** the model was downloaded and verified on an earlier launch and the user opens the app
- **THEN** the Receipts list is shown, without the setup screen

#### Scenario: Back from setup
- **WHEN** the setup screen is shown and the user presses back
- **THEN** the app closes
