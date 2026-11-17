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
- **WHEN** the scan is finished and its text is being read
- **THEN** a progress indicator is shown, and the user can cancel and return to the Receipts list

### Requirement: Scanned images are not kept
The app SHALL delete the scanned image once its text has been read, whether or not a receipt is saved. The image SHALL NOT be stored with the receipt.

#### Scenario: Image deleted after reading
- **WHEN** a scan has been read and the review form is shown
- **THEN** the app's storage no longer contains the scanned image

#### Scenario: Reading fails
- **WHEN** reading the scanned image fails
- **THEN** the image is deleted and the user is offered to try again or enter the receipt manually

### Requirement: Date candidates
The app SHALL find the dates printed on a scanned receipt, numeric (03/02/2026, 2026-02-03) or with month names (3 Feb 2026). Labels glued to the date, spaced separators, a time right after it and digits OCR misreads as letters SHALL NOT stop a date being found. When the day/month order is ambiguous, the device locale's order SHALL decide. Dates in the future SHALL NOT be suggested. The first valid date on the receipt SHALL be preselected.

#### Scenario: Day-first locale
- **WHEN** the device locale is en-IN and the receipt shows "03/02/2026"
- **THEN** 3 February 2026 is suggested

#### Scenario: Month-first locale
- **WHEN** the device locale is en-US and the receipt shows "03/02/2026"
- **THEN** 2 March 2026 is suggested

#### Scenario: Date glued to its label
- **WHEN** the device locale is en-IN and the receipt shows "Date:-12/11/2026"
- **THEN** 12 November 2026 is preselected instead of today

#### Scenario: Messy printed dates
- **WHEN** the receipt shows "Dt.12/11/26", "12 / 11 / 2026", "12/11/2026,10:30", "12 Nov' 26" or "12/1l/2O26"
- **THEN** 12 November 2026 is suggested for each

#### Scenario: Future date ignored
- **WHEN** the receipt shows a date after today (for example a "valid until" date) and a past purchase date
- **THEN** only the past date is suggested

### Requirement: Total candidates
The app SHALL find the amounts printed on a scanned receipt, in the same exact form as manual entry (minor units, without rounding). A whole number counts as an amount when it is on a line labelled as a total or next to a currency symbol or code; times of day (for example "1.00 to 4.00 pm") SHALL NOT be read as amounts. Handwritten amounts may not be readable; when no amount is found, the total is left for the user to enter. The preselected total SHALL be the amount actually paid: a line labelled with the payable amount (for example "NET AMOUNT", "NET PAYABLE", "AMOUNT DUE", "BALANCE DUE", "GRAND TOTAL") SHALL win over a line labelled only "TOTAL" (or "SUMME", "GESAMT"), wherever each appears; among lines of the same kind, the last one wins. Gross amounts, sub-totals and tax totals SHALL NOT be preselected. When no line is labelled, the largest amount that isn't a gross amount or sub-total SHALL be preselected. All other amounts SHALL be offered as alternatives, largest first.

#### Scenario: Labelled total
- **WHEN** the receipt lists items of 120.00 and 80.00, "SUBTOTAL 200.00", "TAX 10.00" and "TOTAL 210.00"
- **THEN** 210.00 is preselected as the total

#### Scenario: Net amount below the gross total
- **WHEN** the receipt shows "NET AMOUNT 950.00" followed by "GROSS TOTAL 1,000.00"
- **THEN** 950.00 is preselected, and 1,000.00 is offered as an alternative

#### Scenario: Payable amount before a plain total
- **WHEN** the receipt shows "Net Payable 950.00" followed by "Total 1,000.00"
- **THEN** 950.00 is preselected

#### Scenario: No label
- **WHEN** the receipt shows the amounts 4.50, 12.00 and 16.50 and no total label
- **THEN** 16.50 is preselected and 12.00 and 4.50 are offered as alternatives

#### Scenario: Whole amount on the total line
- **WHEN** the receipt shows "TOTAL 3050" and the default currency is INR
- **THEN** a total of 3050.00 INR is preselected

#### Scenario: Shop hours
- **WHEN** the receipt's only numbers with decimals are in "Exchange Time 1.00 to 4.00 pm"
- **THEN** no total is suggested

#### Scenario: Indian grouping
- **WHEN** the receipt shows "Grand Total ₹1,23,456.50"
- **THEN** a total of 123456.50 INR is preselected

### Requirement: Currency from the receipt
When the receipt prints a currency symbol or code next to the preselected total (for example ₹, Rs, INR, $, €, £), the review form SHALL use that currency. Otherwise it SHALL use the default currency for new receipts.

#### Scenario: Symbol on the receipt
- **WHEN** the device's default currency is INR and the receipt's total reads "€ 4,50"
- **THEN** the review form shows a total of 4.50 EUR

#### Scenario: No symbol
- **WHEN** the receipt's total has no currency symbol or code
- **THEN** the review form uses the default currency

### Requirement: Merchant candidate
The app SHALL suggest as merchant a line near the top of the receipt that reads like a name: it has letters, and it isn't a document title (for example "RETAIL INVOICE", "TAX INVOICE", "CASH MEMO", "ORIGINAL FOR RECIPIENT"), a date, an amount, a phone number, a web or email address, or a tax registration line. When one such line is printed clearly larger than the rest of the receipt, as shop names usually are, it SHALL be preferred; otherwise the first such line SHALL be suggested. Text printed much larger than the text beside it SHALL NOT be joined with it into one line. A leading "Welcome to" SHALL be dropped.

#### Scenario: Name at the top
- **WHEN** the receipt's first lines are "CORNER SHOP", "12 High Street" and "Tel 020 7946 0000"
- **THEN** "CORNER SHOP" is suggested as merchant

#### Scenario: Document title above a large shop name
- **WHEN** the receipt starts with "RETAIL INVOICE" and an address block, with "HIT-MAN" printed large beside the address
- **THEN** "HIT-MAN" is suggested as merchant

### Requirement: Category from the shop's name
The app SHALL suggest a category when the merchant name, or failing that one of the top eight lines of the receipt, names the kind of shop (for example "Wear", "Fashion" or "Boutique" for Shopping; "Medical", "Pharmacy" or "Chemist" for Health; "Supermarket" or "General Store" for Groceries; "Cafe" or "Restaurant" for Dining). Words SHALL match whole words only. When nothing names the kind of shop, no category SHALL be suggested.

#### Scenario: Clothing shop
- **WHEN** the scanned receipt's merchant is "Men's Wear"
- **THEN** the review form's category is Shopping

#### Scenario: Medical store
- **WHEN** the scanned receipt's merchant is "Sai Medicals"
- **THEN** the review form's category is Health

#### Scenario: No hint
- **WHEN** the scanned receipt's merchant is "Corner Shop" and no top line names the kind of shop
- **THEN** the review form's category is Other

### Requirement: Review before saving
After a scan, the receipt form SHALL open titled "Review receipt", prefilled with the preselected merchant, date, total and currency, and with the suggested category, or Other when none was suggested. The user SHALL be able to switch the date or total to another candidate with one tap, edit any field, and then save with the same validation as manual entry. Nothing SHALL be saved until the user taps Save. Going back from the review form SHALL return to the Receipts list, not to the scanner.

#### Scenario: Confirm a scan
- **WHEN** the review form shows "Corner Shop", today's date and 4.50, and the user taps Save
- **THEN** the receipt is saved and appears in the Receipts list

#### Scenario: Pick another total
- **WHEN** the preselected total is 210.00 and the user taps the 200.00 candidate
- **THEN** the total field shows 200.00

#### Scenario: Discard a scan
- **WHEN** the user goes back from the review form without saving
- **THEN** the Receipts list is shown and no receipt is created

#### Scenario: Nothing recognised
- **WHEN** the scan contains no readable text, date or amount
- **THEN** the review form opens with the empty fields and defaults of manual entry, and a message says nothing could be read

### Requirement: Review survives configuration changes
The review form, including its candidates and any edits, SHALL survive configuration changes such as rotation.

#### Scenario: Rotate while reviewing
- **WHEN** the user switches the total to another candidate and rotates the device
- **THEN** the review form still shows the chosen total and the other candidates
