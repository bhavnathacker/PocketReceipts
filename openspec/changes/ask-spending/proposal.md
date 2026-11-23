# Proposal

## Why

Ask is still a placeholder, yet the main reason to keep receipts is to know where the money went. Users want to type questions such as "How much did I spend on groceries in October?", "How much is my total spend in 2025?" or "How much on shopping this quarter?" and get an exact answer, offline, without the question or their receipts leaving the phone. Gemma already runs on the phone for scanning, so it can also read these questions.

## What Changes

- The Ask destination shows a question field with example questions instead of the placeholder.
- Gemma, on the phone, turns the question into a structured query: an optional category and a period (a month, a quarter, a year, a date range, or all time). Gemma never does the arithmetic.
- The app checks that query and adds up the matching receipts exactly from the receipts database, in whole minor units.
- The answer states the category and the dates that were used (for example "Groceries, 1–31 October 2026"), the total, and the number of receipts, so the user can see how the question was read. Receipts in different currencies are totalled separately per currency, with no conversion.
- A question that isn't about spending in a category and period, or that Gemma's reply can't be checked for, gets a "can't answer that" message with example questions. No answer is guessed.
- The model is loaded while Ask is shown, so it's ready by the time the question is typed. It's then released by the same rules as for scanning.
- Questions and answers aren't stored or sent anywhere.
- Out of scope: questions about merchants, averages, comparisons, the largest purchase, or lists of receipts; currency conversion; a history of questions; voice input.

## Capabilities

### New Capabilities
- `spending-questions`: answering spending questions typed in plain language on the Ask destination. This covers how a question is read into a category and period, how the total is worked out, how the answer is shown, and what happens with questions it can't answer.

### Modified Capabilities
- `app-navigation`: the "Placeholder destination content" requirement no longer covers Ask, which now shows spending questions. Only Settings stays a placeholder.

## Impact

- **`:feature:ask`**: a new Ask screen and ViewModel replace the placeholder.
- **`:domain`**:
  - New use cases: one answers a question, another checks Gemma's reply.
  - New models for the spending query and the answer.
  - A repository interface for reading questions.
  - A query for spending totals on `ReceiptRepository`.
  - `KeepReceiptReaderReadyUseCase` becomes a general "keep the model ready" use case for both scanning and Ask.
- **`:data:ai`**:
  - A new prompt builder and reply parser for questions.
  - A repository that uses the shared `LiteRtModelRuntime` (a new conversation for each question).
  - The receipt prompt and its reply limit stay exactly as they are.
- **`:data:receipts`**: a DAO query that sums totals for each currency for a date range and an optional category. There's no schema change.
- **`:core:ui`**: gains the money formatter and the category labels, which move out of `:feature:receipts` so both features can use them.
- **No new dependencies or permissions.** No network use, and the privacy checks are unchanged.
