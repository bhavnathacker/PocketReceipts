## Purpose

Defines how the user asks the Ask destination about their spending in plain language and gets an exact total, worked out on the phone from their saved receipts.

## ADDED Requirements

### Requirement: Ask a spending question
The Ask destination SHALL show a text field for a question, an Ask action, and example questions. Tapping an example SHALL put its text in the field. The Ask action SHALL be disabled while the field is blank or a question is being answered. The field SHALL accept up to 200 characters.

#### Scenario: Ask screen opened
- **WHEN** the user opens Ask
- **THEN** an empty question field, a disabled Ask action and example questions such as "How much did I spend on groceries in October?" are shown

#### Scenario: Example chosen
- **WHEN** the user taps the example "How much did I spend on shopping this quarter?"
- **THEN** the question field contains that text and the Ask action is enabled

### Requirement: Question read by Gemma
The app SHALL pass the question and today's date to the on-device Gemma model, which SHALL return one optional category and one period: a calendar month, a calendar quarter, a calendar year, a date range, or all time. A question that names no category SHALL cover all categories. The question and Gemma's reply SHALL stay on the device and SHALL NOT be stored.

#### Scenario: Category and month
- **WHEN** today is 21 November 2026 and the user asks "How much did I spend on groceries in October?"
- **THEN** the answer covers Groceries from 1 to 31 October 2026

#### Scenario: Whole year, all categories
- **WHEN** the user asks "How much is total spend in 2025?"
- **THEN** the answer covers all categories from 1 January to 31 December 2025

#### Scenario: Relative quarter
- **WHEN** today is 21 November 2026 and the user asks "How much in shopping this quarter?"
- **THEN** the answer covers Shopping from 1 October to 31 December 2026

### Requirement: Month without a year
A month named without a year SHALL mean its most recent occurrence that has started on or before today.

#### Scenario: Month later in the year
- **WHEN** today is 21 November 2026 and the user asks "How much did I spend in December?"
- **THEN** the answer covers 1 to 31 December 2025

### Requirement: Gemma's reading is checked
Before working out a total, the app SHALL check Gemma's reply. A category outside the app's categories, a month outside 1–12, a quarter outside 1–4, a date that can't be parsed, or a range that ends before it starts SHALL make the question unanswerable. The app SHALL NOT widen or guess a value it discards.

#### Scenario: Unknown category
- **WHEN** Gemma returns the category "PETS"
- **THEN** the "can't answer" message is shown, not a total for all categories

#### Scenario: Range backwards
- **WHEN** Gemma returns a range from 31 October 2026 to 1 October 2026
- **THEN** the "can't answer" message is shown

### Requirement: Exact totals from saved receipts
The app SHALL add up the totals of the saved receipts whose purchase date is in the period (both ends included) and, when a category was read, whose category matches. Amounts SHALL be added exactly in minor units. Gemma SHALL NOT compute or see any amount.

#### Scenario: Several receipts
- **WHEN** October 2026 has Groceries receipts of 120.50 INR and 79.50 INR and a Dining receipt of 300.00 INR, and the user asks about groceries in October
- **THEN** the total is 200.00 INR from 2 receipts

#### Scenario: Receipt edited after asking
- **WHEN** the user asks again after changing one of those receipts' categories
- **THEN** the new answer reflects the change

### Requirement: Totals per currency
Receipts in different currencies SHALL be totalled separately, with one total per currency and no conversion. The totals SHALL be listed with the currency that has the most receipts first.

#### Scenario: Two currencies
- **WHEN** the period has 3 INR receipts totalling 1,500.00 INR and 1 EUR receipt of 12.30 EUR
- **THEN** the answer shows ₹1,500.00 from 3 receipts and €12.30 from 1 receipt, INR first

### Requirement: Answer shows how the question was read
The answer SHALL state the category (or "All spending"), the period's first and last dates, each total in the user's locale format, and the number of receipts counted.

#### Scenario: Answer shown
- **WHEN** the question is read as Groceries in October 2026 and 2 receipts total 200.00 INR
- **THEN** the answer shows "Groceries", the dates "1 – 31 Oct 2026" (as the locale writes a date range), "₹200.00" and "2 receipts"

#### Scenario: Nothing spent
- **WHEN** no saved receipt matches the category and period
- **THEN** the answer says no receipts were found for that category and period, rather than showing a total of zero in some currency

### Requirement: Questions it can't answer
When the question isn't about how much was spent in a category and period, or Gemma's reply can't be read or fails the checks, the app SHALL say it can't answer that question and show the example questions. It SHALL NOT show a total.

#### Scenario: Unsupported question
- **WHEN** the user asks "Which shop did I visit most?"
- **THEN** the "can't answer" message and the example questions are shown

### Requirement: Answering progress and failure
While a question is being answered, Ask SHALL show that it's working. If the model can't be run, Ask SHALL say the question couldn't be answered and keep the question so the user can try again.

#### Scenario: Working
- **WHEN** the user taps Ask
- **THEN** a progress indicator is shown until the answer appears

#### Scenario: Model fails
- **WHEN** the model fails while reading the question
- **THEN** an error message is shown and the question stays in the field

### Requirement: Model prepared while Ask is shown
The app SHALL start loading the on-device model when Ask is shown and keep it loaded while Ask stays shown, so a question can be read without waiting for the load. After Ask is left, the model SHALL be released by the same rules that apply after scanning.

#### Scenario: Question asked after the model loaded
- **WHEN** the user opens Ask, types a question for longer than the model takes to load, and taps Ask
- **THEN** the question is read without first loading the model

#### Scenario: Ask left
- **WHEN** the user leaves Ask and doesn't use the model again
- **THEN** the model is released 2 minutes later, or at once if the app goes to the background

### Requirement: Ask survives configuration changes
The question text and the answer shown SHALL survive configuration changes such as rotation.

#### Scenario: Rotate with an answer
- **WHEN** an answer is shown and the user rotates the device
- **THEN** the same question and answer are still shown
