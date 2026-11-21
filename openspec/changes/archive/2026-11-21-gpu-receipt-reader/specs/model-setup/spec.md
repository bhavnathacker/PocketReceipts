# Spec Delta

## MODIFIED Requirements

### Requirement: Phone can't run the model
When a verified model can't be loaded on the phone (for example, not enough memory), setup SHALL say that this phone can't run the receipt reader, and SHALL offer Retry. The app SHALL stay on the setup screen. The model counts as unloadable only when it can't be loaded on either the graphics processor or the CPU.

#### Scenario: Model fails to load
- **WHEN** the model file is verified but loading it fails
- **THEN** setup says this phone can't run the receipt reader and shows Retry

#### Scenario: Graphics processor fails, CPU works
- **WHEN** the model can't be loaded on the graphics processor but loads on the CPU
- **THEN** setup completes and the Receipts list is shown
