# Spec Delta

## ADDED Requirements

### Requirement: Receipt data encrypted at rest
Receipt data stored on the device SHALL be encrypted. The encryption key SHALL be protected by the device's hardware-backed keystore where available, and SHALL NOT be stored in plain form or leave the device.

#### Scenario: Database file read directly
- **WHEN** someone copies the app's receipt database file off a device and opens it without the app's key
- **THEN** no receipt field (merchant, amount, date, category, note) can be read from it

#### Scenario: App reopened
- **WHEN** the user reopens the app on the same device
- **THEN** their receipts open without asking for any password

### Requirement: Receipt data excluded from backup and transfer
Receipt data and its encryption key material SHALL be excluded from cloud backup and from device-to-device transfer.

#### Scenario: Cloud backup
- **WHEN** the system makes a cloud backup of the app
- **THEN** the backup contains neither the receipt database nor its key material

#### Scenario: Device-to-device transfer
- **WHEN** the user moves to a new phone with a device-to-device transfer
- **THEN** the new phone does not receive the receipt database or its key material, and the app starts with an empty receipt list
