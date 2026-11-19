# receipt-capture Specification

## Purpose

Defines how a paper receipt becomes a saved receipt without typing everything: the user scans it, the app reads its text on the device, suggests the date, total and merchant, and the user confirms them on a review form.

## Requirements

### Requirement: Start a scan
The user SHALL be able to start scanning a receipt from the Receipts destination. The scan SHALL capture a single page, and the user SHALL be able to retake it, crop it, or import an image from the gallery instead of using the camera. While scanning and reviewing, Receipts SHALL stay selected in the navigation UI.

#### Scenario: Scan from the list
- **WHEN** the user taps "Scan receipt" on the Receipts destination
- **THEN** the document scanner opens

#### Scenario: Scan cancelled
- **WHEN** the user closes the scanner without taking a scan
- **THEN** the Receipts list is shown again and no receipt is created

### Requirement: Scanning unavailable
When scanning cannot run on the device, the app SHALL say so and offer manual entry instead.

#### Scenario: Scanner not available
- **WHEN** the user taps "Scan receipt" and the scanner can't be started (for example, Google Play services is missing)
- **THEN** a message says scanning isn't available on this device, with an action that opens the empty add form

### Requirement: On-device text recognition
The text of a scanned receipt SHALL be read on the device. The scanned image and its text SHALL NOT be sent off the device. Reading SHALL work without an internet connection. (The scanner itself is installed once by Google Play services, on first use.)

#### Scenario: Offline scan
- **WHEN** the scanner has been installed, the device has no network connection, and the user scans a receipt
- **THEN** the review form opens with values read from the receipt

#### Scenario: Reading in progress
- **WHEN** the scan is finished and its text is being read or Gemma is reading its fields
- **THEN** a progress indicator says the receipt is being read, and the user can cancel and return to the Receipts list

### Requirement: Scanned images are not kept
The app SHALL delete the scanned image once its text has been read, whether or not a receipt is saved. The image SHALL NOT be stored with the receipt.

#### Scenario: Image deleted after reading
- **WHEN** a scan has been read and the review form is shown
- **THEN** the app's storage no longer contains the scanned image

#### Scenario: Reading fails
- **WHEN** reading the scanned image fails
- **THEN** the image is deleted and the user is offered to try again or enter the receipt manually

#### Scenario: Gemma fails
- **WHEN** the text was recognized but Gemma fails or returns an answer that can't be read
- **THEN** the image is deleted and the user is offered to try again or enter the receipt manually

### Requirement: Review before saving
After a scan, the receipt form SHALL open titled "Review receipt", prefilled with Gemma's merchant, date, total, currency and category, or the manual-entry defaults for any field Gemma didn't return (today's date, the default currency, Other). The user SHALL be able to edit any field and then save with the same validation as manual entry. Nothing SHALL be saved until the user taps Save. Going back from the review form SHALL return to the Receipts list, not to the scanner.

#### Scenario: Confirm a scan
- **WHEN** the review form shows "Corner Shop", today's date and 4.50, and the user taps Save
- **THEN** the receipt is saved and appears in the Receipts list

#### Scenario: Pick another total
- **WHEN** the review form shows a total of 210.00 and the user changes it to 200.00 and taps Save
- **THEN** the receipt is saved with a total of 200.00

#### Scenario: Discard a scan
- **WHEN** the user goes back from the review form without saving
- **THEN** the Receipts list is shown and no receipt is created

#### Scenario: Nothing recognised
- **WHEN** the scan contains no readable text, or Gemma returns no merchant, date or total
- **THEN** the review form opens with the empty fields and defaults of manual entry, and a message says nothing could be read

### Requirement: Review survives configuration changes
The review form, including Gemma's values and any edits, SHALL survive configuration changes such as rotation.

#### Scenario: Rotate while reviewing
- **WHEN** the user edits the prefilled total and rotates the device
- **THEN** the review form still shows the edited total and the other prefilled values

### Requirement: Receipt fields read by Gemma
The app SHALL pass the recognized text of a scan to the on-device Gemma model, which SHALL return one merchant, purchase date, total, currency and category. The review form SHALL be prefilled with these values. Recognized text and Gemma's answer SHALL stay on the device. There SHALL be no rule-based extraction: when Gemma returns no value for a field, that field SHALL be left at its manual-entry default.

#### Scenario: Printed pharmacy cash memo
- **WHEN** the scanned receipt is the Bharat Medical Stores cash memo whose text reads "BHARAT MEDICAL STORES", "31-0ct-26", "Gross Total 880.00" and "NET 764.00"
- **THEN** the review form shows merchant "BHARAT MEDICAL STORES", date 31 October 2026, total 764.00 INR and category Health

#### Scenario: Amount actually paid
- **WHEN** the receipt shows a gross total and a lower net or payable amount
- **THEN** the total is the net or payable amount

#### Scenario: Currency on the receipt
- **WHEN** the device's default currency is INR and the receipt's total reads "€ 4,50"
- **THEN** the review form shows a total of 4.50 EUR

#### Scenario: Field not found
- **WHEN** Gemma finds no total on the receipt
- **THEN** the total field is empty and the currency is the default currency

### Requirement: Gemma's answer is checked
Before prefilling, the app SHALL discard any value Gemma returns that isn't valid for the form: a date that can't be parsed or is after today, a currency that isn't an ISO 4217 code, a total that isn't a positive amount, or a category outside the app's categories. A merchant longer than the form allows SHALL be cut to that length. A discarded value SHALL leave its field at the manual-entry default.

#### Scenario: Future date
- **WHEN** Gemma returns a purchase date after today
- **THEN** the review form's date is today

#### Scenario: Unknown category
- **WHEN** Gemma returns a category that isn't one of the app's categories
- **THEN** the review form's category is Other

#### Scenario: Not an amount
- **WHEN** Gemma returns a total that isn't a positive amount
- **THEN** the total field is empty
