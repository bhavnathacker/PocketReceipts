# Design

## Context

- **Ask is a placeholder.** `:feature:ask` holds only `AskScreen` (a `PlaceholderScreen`) and `AskKey`.
- **The model is shared.** Gemma 4 E2B runs through the shared `LiteRtModelRuntime` in `:data:ai`:
  - It has one engine, holders, a single inference thread, and GPU→CPU fallback.
  - It releases the engine 2 minutes after its last use, or at once in the background when nothing holds it.
  - `LiteRtReceiptExtractionRepository` shows the pattern: a new `Conversation` for each request, greedy sampling, a short `maxOutputToken`, and a JSON reply parsed between the first `{` and the last `}`.
- **The receipt prompt is fixed.** The owner chose not to change it or its reply limit, so Ask gets its own prompt, parser and repository.
- **The scanner keeps the model ready.** `CaptureViewModel` calls `KeepReceiptReaderReadyUseCase` in `viewModelScope`. Behind it, `ModelRuntimeRepository.keepLoaded()` holds the engine until it is cancelled.
- **Receipts storage.** Receipts live in Room (`receipts` table):
  - `purchase_date` holds epoch days and is indexed together with `created_at`.
  - `total_minor` is a `Long`, `currency_code` holds the ISO code, and `category` holds a stable key (`ReceiptCategoryMapper`).
  - `ReceiptRepository` only exposes `monitorReceipts()`, `monitorReceipt(id)` and the write methods.
- **Formatting lives in `:feature:receipts`.** `MoneyFormatter` and `ReceiptCategoryLabelMapper`, with the `category_*` strings, are internal there. Features can't depend on each other, but `ModuleDependencyRules` allows `:core:*` to depend on `:domain`.

## Goals / Non-Goals

**Goals:**
- Answer "how much in category X during period P" questions exactly, offline.
- Make Gemma's reading of the question visible, so a wrong reading is obvious.
- Reuse the shared engine without changing how scanning behaves.

**Non-Goals:**
- Questions about merchants, averages, comparisons, the largest purchase, or listing receipts.
- Currency conversion, a saved question history, voice input, streamed replies.
- Any change to the receipt prompt, its reply limit, or the database schema.

## Decisions

### 1. Gemma reads the question; the app does the arithmetic

Gemma only turns the text into `{category, period}`. The app then sums the amounts in SQL on `Long` minor units. Language models are unreliable at arithmetic and can't see the whole receipt table. Putting receipts into the prompt would also be slow, because of the prompt length.

*Alternative:* fixed choosers (category dropdown plus period picker), with no model. They're more reliable, but they don't answer typed questions, which is what the user asked for. The example chips cover the "I don't know what to type" case.

### 2. The period is a kind, not a pair of dates

Gemma returns one of these forms:

```json
{"category": "GROCERIES" | null,
 "period": {"type": "month", "year": 2026 | null, "month": 10}
         | {"type": "quarter", "year": 2026, "quarter": 4}
         | {"type": "year", "year": 2025}
         | {"type": "range", "from": "2026-11-01", "to": "2026-11-21"}
         | {"type": "all"}}
```

Or, for a question it can't map: `{"unsupported": true}`.

The app turns the kind into inclusive dates itself, with `YearMonth.atEndOfMonth()` and quarter → months `3q-2 … 3q`. This avoids errors from the model working out month-end and quarter boundaries.

The prompt gives today's date, explains that "this/last month/quarter/year" are relative to today, and uses `range` for "last N days" and "this week".

The month-without-year rule (the most recent occurrence that has started) is applied in code, not left to Gemma. The prompt asks Gemma to return `"year": null` when the question names no year. `SpendingQueryChecker` then picks the current year, or the previous one if that month hasn't started yet. A month whose year is stated is kept as it is, even if it is in the future; the answer then finds no receipts.

### 3. Checking lives in `:domain`, the same way as `ReceiptFieldsChecker`

`:data:ai` parses the reply into raw strings and numbers (`InterpretedQuestion`). `SpendingQueryChecker` (domain) validates them and returns a `SpendingQuery(category: ReceiptCategory?, from: LocalDate?, to: LocalDate?)` (both dates `null` for all time; the DAO then gets `Long.MIN_VALUE`..`Long.MAX_VALUE`), or null. It rejects:
- an unknown category,
- a month outside 1–12 or a quarter outside 1–4,
- a year outside 1900–9999,
- dates that can't be parsed,
- a range with `from > to`.

Rejecting the reply means "can't answer"; the app never falls back to all categories.

### 4. Totals are summed in SQL, grouped by currency

A new DAO query:

```sql
SELECT currency_code, SUM(total_minor) AS total_minor, COUNT(*) AS receipt_count
FROM receipts
WHERE purchase_date BETWEEN :fromEpochDay AND :toEpochDay
  AND (:category IS NULL OR category = :category)
GROUP BY currency_code
ORDER BY receipt_count DESC, currency_code
```

SQLite `SUM` over integers is exact 64-bit arithmetic. The query uses the `purchase_date` index.

`ReceiptRepository` gains `suspend fun spendingTotals(from, to, category): List<SpendingTotal>`, where `SpendingTotal` is `(money: Money, receiptCount: Int)`.

It's a one-shot `suspend` call, not a `Flow`: an answer reflects the receipts at the moment the question was asked, and asking again recomputes it (spec scenario "Receipt edited after asking").

### 5. Use case and result shape

`AnswerSpendingQuestionUseCase(question): SpendingAnswer` works in these steps:
1. Trim the question.
2. Get today from `GetTodayUseCase`.
3. Call `SpendingQuestionRepository.interpret(question, today)`.
4. Run `SpendingQueryChecker`.
5. Call `ReceiptRepository.spendingTotals`.

```kotlin
sealed interface SpendingAnswer {
    data class Totals(val query: SpendingQuery, val totals: List<SpendingTotal>) : SpendingAnswer // empty = nothing spent
    data object Unanswerable : SpendingAnswer
}
```

Failures are handled like this:
- `UnreadableReplyException`-style parse failures map to `Unanswerable`.
- Engine or model failures are thrown. The ViewModel shows them as an error and keeps the question.
- `CancellationException` is rethrown, never swallowed.

### 6. The `:data:ai` side

There are three new classes:
- `SpendingQuestionPromptBuilder`: the category list comes from `ReceiptCategory.entries`, and today's ISO date is included. Short worked examples cover month, quarter, year and unsupported questions.
- `SpendingQuestionReplyParser`: uses lenient `Json`, ignores unknown keys, and takes the substring from the first `{` to the last `}`.
- `LiteRtSpendingQuestionRepository`: uses `runtime.withEngine` with a new `Conversation`, greedy `SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)` and `maxOutputToken = 64`. The reply is under 40 tokens.

The question is placed after the instructions, inside a clearly marked `Question:` line, so text in the question can't change the instructions. The worst a hostile question can do is produce an unsupported reply or a checked query. The model has no other effects.

Nothing is logged except the elapsed time, matching "Receipt read in %s". The question text is never logged.

### 7. Keeping the model ready on Ask

`KeepReceiptReaderReadyUseCase` is renamed to `KeepModelReadyUseCase` and moved to `domain.ai`. Its behaviour is unchanged, and `CaptureViewModel` and its tests are updated for the new name.

`AskViewModel` doesn't hold the model for its whole lifetime. A tab's ViewModel can outlive the moment the user leaves it, so this would pin about 1 GB. Instead:
- `AskScreen` uses `LifecycleStartEffect` to call `onShown()` and `onHidden()`.
- `onShown()` launches a `keepModelReady` job.
- `onHidden()` cancels it.

When the app goes to the background, the screen stops, the hold ends, and the runtime's existing rule releases the engine at once. Leaving Ask in the foreground starts the existing 2-minute idle timer. A question already being answered holds the engine through `withEngine`, so hiding Ask mid-answer doesn't unload it.

### 8. UI state

`AskUiState(question: String, isAnswering: Boolean, answer: AskAnswerUiModel?, error: Boolean)`:
- The question is kept in `SavedStateHandle`.
- The answer lives in the ViewModel, so it survives rotation. After process death it is recomputed only when the user asks again.
- `AskAnswerUiModel` is either:
  - `Totals(categoryLabel @StringRes or "All spending", periodText, lines: List<(amountText, countText)>)`, or
  - `NothingSpent(categoryLabel, periodText)`, or
  - `Unanswerable`.
- A `SpendingAnswerUiMapper` builds it:
  - Periods are shown as compact date ranges in the user's locale, for example "1–31 Oct 2026" and "1 Oct – 31 Dec 2026". This uses `DateTimeFormatter` with `FormatStyle.MEDIUM`, with the shared year and month collapsed. If collapsing proves awkward across locales, the full "1 Oct 2026 – 31 Oct 2026" is acceptable.
  - Receipt counts use a plurals resource.
- The examples are string resources:
  - "How much did I spend on groceries in October?"
  - "How much is my total spend in 2025?"
  - "How much did I spend on shopping this quarter?"
  - "How much did I spend last month?"

### 9. Shared formatting moves to `:core:ui`

`MoneyFormatter`, `ReceiptCategoryLabelMapper` and the nine `category_*` strings move into `:core:ui` and become public. This needs `:core:ui` to depend on `:domain`, which the rules allow.

`:feature:receipts` imports them from there. Its behaviour and tests don't change; the tests move along with the classes.

## Risks / Trade-offs

- **Gemma misreads a question**, for example "October" as the wrong year, or "food" as Dining instead of Groceries. *Mitigations:*
  - The answer always shows the category and dates it used.
  - The month rule is enforced in code.
  - The phone check runs the user's own example questions.
- **First question after opening Ask on a cold phone waits for the load** (about 25–30 s on the GPU). *Mitigation:* loading starts as soon as Ask is shown. The progress indicator covers any remaining wait.
- **About 1 GB of memory while Ask is visible.** This is accepted, the same as while the scanner is open. The engine is released on background and after 2 minutes idle.
- **Ask and scanning share one engine.** The runtime serialises requests on its single thread, so they can't run concurrently. In practice they're never on screen together.
- **No currency conversion.** Mixed-currency spending shows several totals. That is deliberate: conversion would need exchange rates, which means network access.
