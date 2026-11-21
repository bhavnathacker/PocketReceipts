# Spec Delta

## ADDED Requirements

### Requirement: Receipt reader uses the graphics processor where supported
The app SHALL run the on-device model on the phone's graphics processor when the phone supports it, and on the main processor (CPU) otherwise. If the model can't be loaded on the graphics processor, or fails there while reading a receipt, the app SHALL use the CPU for the rest of its run and SHALL read that receipt again on the CPU. Which processor is used SHALL NOT change the values read from a receipt.

#### Scenario: Phone with a supported graphics processor
- **WHEN** the user scans a receipt on a phone whose graphics processor is supported
- **THEN** the receipt is read on the graphics processor

#### Scenario: Phone without a supported graphics processor
- **WHEN** the user scans a receipt on a phone without a supported graphics processor
- **THEN** the receipt is read on the CPU, as before, without trying the graphics processor first

#### Scenario: Graphics processor fails to load the model
- **WHEN** loading the model on the graphics processor fails
- **THEN** the model is loaded on the CPU and the receipt is read there

#### Scenario: Graphics processor fails while reading
- **WHEN** reading a receipt on the graphics processor fails
- **THEN** the same receipt is read again on the CPU, the review form opens with its values, and later receipts are read on the CPU until the app is restarted

#### Scenario: Both processors fail
- **WHEN** reading fails on the CPU too
- **THEN** the app offers to try again or enter the receipt manually, as for any reading failure
