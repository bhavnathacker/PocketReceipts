# Spec Delta

## Purpose

Defines what a receipt is and what users can do with their receipts: browse them, view one, add one by hand, edit it and delete it. These receipts are the data that capture, AI extraction and Ask build on later.

## ADDED Requirements

### Requirement: Receipt fields
A receipt SHALL have a merchant name, a purchase date, a total amount with its currency, and a category. It MAY have a note. The category SHALL be one of: Groceries, Dining, Transport, Shopping, Bills, Health, Entertainment, Travel, Other.

#### Scenario: Complete receipt
- **WHEN** the user views a saved receipt
- **THEN** its merchant, purchase date, total with currency, category and note (if any) are shown

#### Scenario: Category choices
- **WHEN** the user picks a category on the receipt form
- **THEN** exactly the nine categories are offered, in the order listed

### Requirement: Amounts are exact
A total SHALL be stored and shown exactly as entered, without floating-point rounding, using the number of decimal places of its currency.

#### Scenario: Decimal currency
- **WHEN** the user saves a total of 12.30 EUR
- **THEN** the receipt shows a total of 12.30 EUR, not 12.3 or 12.299999

#### Scenario: Digit grouping in the device locale
- **WHEN** the device locale is en-IN and the user enters a total of 1,23,456.50 INR
- **THEN** the receipt is saved and shown with a total of ₹1,23,456.50

#### Scenario: Currency without minor units
- **WHEN** the user enters a total in JPY
- **THEN** the form accepts whole amounts only and rejects a decimal part

### Requirement: Currency per receipt
Each receipt SHALL carry its own currency. New receipts SHALL default to the currency of the device's locale, and the user SHALL be able to choose any ISO 4217 currency instead.

#### Scenario: Default currency
- **WHEN** the device locale is en-GB and the user starts a new receipt
- **THEN** the currency is preset to GBP

#### Scenario: Different currency
- **WHEN** the user changes the currency of a new receipt to USD and saves it
- **THEN** the receipt shows its total in USD, while other receipts keep their own currencies

### Requirement: Receipt list
The Receipts destination SHALL list all receipts, newest purchase date first. Receipts with the same purchase date SHALL be ordered most recently added first. Each row SHALL show merchant, purchase date, total with currency, and category.

#### Scenario: Ordering
- **WHEN** receipts exist dated 3 March, 1 March and 3 March (added in that order)
- **THEN** the list shows the second 3 March receipt, then the first 3 March receipt, then 1 March

#### Scenario: List updates after a change
- **WHEN** the user adds, edits or deletes a receipt and returns to the list
- **THEN** the list reflects the change without a manual refresh

### Requirement: Empty receipt list
When no receipts exist, the Receipts destination SHALL show an empty state that explains there are no receipts yet and offers a way to add one.

#### Scenario: First launch
- **WHEN** the user opens Receipts and no receipts exist
- **THEN** an empty-state message and an add action are shown instead of a list

### Requirement: Receipt detail
Selecting a receipt in the list SHALL open a detail screen showing all of its fields, with actions to edit and delete it.

#### Scenario: Open detail
- **WHEN** the user selects a receipt in the list
- **THEN** a detail screen for that receipt is shown, with edit and delete actions

#### Scenario: Back from detail
- **WHEN** the user is on a receipt's detail screen and goes back
- **THEN** the receipt list is shown

### Requirement: Manual entry
The user SHALL be able to add a receipt by hand from the Receipts destination, using a form for all receipt fields. A new receipt SHALL default to today's date, the default currency and the category Other.

#### Scenario: Add a receipt
- **WHEN** the user opens the add form, enters merchant "Corner Shop" and total 4.50, and saves
- **THEN** the form closes and "Corner Shop" appears in the list with today's date, 4.50 in the default currency and category Other

#### Scenario: Leave without saving
- **WHEN** the user opens the add form, enters data and goes back without saving
- **THEN** no receipt is created

### Requirement: Receipt validation
The form SHALL NOT save a receipt unless the merchant is non-blank (at most 100 characters after trimming), the total is greater than zero, and the purchase date is not in the future. The note SHALL be at most 500 characters. Each invalid field SHALL show its own error message.

#### Scenario: Blank merchant
- **WHEN** the user tries to save with a blank or whitespace-only merchant
- **THEN** the receipt is not saved and the merchant field shows an error

#### Scenario: Zero or missing total
- **WHEN** the user tries to save with an empty, zero or non-numeric total
- **THEN** the receipt is not saved and the total field shows an error

#### Scenario: Future date
- **WHEN** the user picks a purchase date after today
- **THEN** the date is not accepted and the form keeps a valid date

#### Scenario: Whitespace trimmed
- **WHEN** the user saves merchant "  Corner Shop  " and a note of only spaces
- **THEN** the receipt is saved with merchant "Corner Shop" and no note

### Requirement: Edit receipt
The user SHALL be able to edit any field of an existing receipt from its detail screen. The form SHALL open with the receipt's current values, and the same validation as manual entry SHALL apply.

#### Scenario: Edit total
- **WHEN** the user edits a receipt's total from 4.50 to 5.00 and saves
- **THEN** the detail screen and the list show 5.00

#### Scenario: Discard edit
- **WHEN** the user changes fields in the edit form and goes back without saving
- **THEN** the receipt keeps its previous values

### Requirement: Delete receipt
The user SHALL be able to delete a receipt from its detail screen, after confirming. Deleting SHALL remove the receipt permanently and return to the list.

#### Scenario: Confirm delete
- **WHEN** the user chooses delete on a receipt and confirms
- **THEN** the receipt is removed, the list is shown, and the receipt no longer appears in it

#### Scenario: Cancel delete
- **WHEN** the user chooses delete on a receipt and cancels the confirmation
- **THEN** the receipt is unchanged and its detail screen stays shown

### Requirement: Receipts persist on the device
Saved receipts SHALL persist across app restarts and process death, until the user deletes them or the app's data is cleared.

#### Scenario: App restarted
- **WHEN** the user saves a receipt, force-stops the app and opens it again
- **THEN** the receipt is in the list with the same values

### Requirement: Form input survives configuration changes
Unsaved input on the add or edit form SHALL survive configuration changes such as rotation.

#### Scenario: Rotate while editing
- **WHEN** the user has typed a merchant and total in the add form and rotates the device
- **THEN** the form still shows the typed values
