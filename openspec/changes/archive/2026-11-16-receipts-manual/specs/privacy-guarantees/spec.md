# Spec Delta

## ADDED Requirements

### Requirement: Receipt data excluded from backup and transfer
Receipt data SHALL be excluded from cloud backup and from device-to-device transfer, so it never leaves the phone.

#### Scenario: Cloud backup
- **WHEN** the system makes a cloud backup of the app
- **THEN** the backup does not contain the receipt database

#### Scenario: Device-to-device transfer
- **WHEN** the user moves to a new phone with a device-to-device transfer
- **THEN** the new phone does not receive the receipt database, and the app starts with an empty receipt list
