# Spec Delta

## MODIFIED Requirements

### Requirement: Empty receipt list
When no receipts exist, the Receipts destination SHALL show an empty state that explains there are no receipts yet and offers ways to add one: scanning a receipt and adding one by hand.

#### Scenario: First launch
- **WHEN** the user opens Receipts and no receipts exist
- **THEN** an empty-state message, a scan action and an add action are shown instead of a list
