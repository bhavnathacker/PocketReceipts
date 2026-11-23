# Tasks

## 1. Shared pieces

- [x] 1.1 Move `MoneyFormatter`, `ReceiptCategoryLabelMapper` and the nine `category_*` strings from `:feature:receipts` to `:core:ui`, and make them public. Add `:domain` to `:core:ui`'s dependencies, then move their tests too (design decision 9). Verify that `./gradlew :feature:receipts:testDebugUnitTest :core:ui:testDebugUnitTest verifyModuleDependencies` passes.
- [x] 1.2 Rename `KeepReceiptReaderReadyUseCase` to `KeepModelReadyUseCase` in `domain.ai`, keeping its behaviour (design decision 7). Update `CaptureViewModel` and the tests. Verify that `./gradlew :domain:test :feature:capture:testDebugUnitTest` passes.

## 2. Domain (`:domain`)

- [x] 2.1 Add these types:
  - `SpendingQuery`, `SpendingTotal` and `SpendingAnswer`.
  - `InterpretedQuestion`: the raw reply, with a category string and a period kind with nullable fields, or unsupported.
  - The `SpendingQuestionRepository` interface.
  - `ReceiptRepository.spendingTotals(from, to, category)`, with KDoc.

  Verify that `./gradlew :domain:compileKotlin` succeeds.
- [x] 2.2 Add `SpendingQueryChecker` (design decisions 2 and 3), writing the tests first. It needs tests for:
  - month, quarter, year, range and all-time periods
  - month with a null year, both before and after today's month
  - a stated future month kept as it is
  - an unknown category
  - a month of 0 or 13, and a quarter of 5
  - an unparseable date and a backwards range
  - unsupported

  Verify that `SpendingQueryCheckerTest` passes.
- [x] 2.3 Add `AnswerSpendingQuestionUseCase` (design decision 5), writing the tests first. It needs tests for:
  - the question is trimmed and today is passed to the repository
  - a checked query reaches `spendingTotals`
  - a reply that fails the check returns `Unanswerable`, without calling `spendingTotals`
  - an unreadable reply returns `Unanswerable`
  - a model failure propagates
  - `CancellationException` is rethrown

  Verify that `AnswerSpendingQuestionUseCaseTest` passes.

## 3. Receipts data (`:data:receipts`)

- [x] 3.1 Add the grouped `SUM`/`COUNT` DAO query (design decision 4) and implement `spendingTotals` in `DefaultReceiptRepository`, using `ReceiptCategoryMapper` for the category key. There's no schema change. Verify both of these:
  - `ReceiptDaoTest` (instrumented, on the emulator) covers:
    - the dates at both ends are included
    - the category filter, and a null category
    - two currencies ordered by count
    - no rows
  - The repository unit test covers the mapping. Verify that `data/receipts/schemas` is unchanged.

## 4. Reading questions (`:data:ai`)

- [x] 4.1 Add `SpendingQuestionPromptBuilder` (design decision 6). Verify with `SpendingQuestionPromptBuilderTest`:
  - Today's date and every `ReceiptCategory` are in the prompt.
  - The question comes last, after a `Question:` marker.
  - The prompt asks for `"year": null` when no year is named.
- [x] 4.2 Add `SpendingQuestionReplyParser`. Verify that `SpendingQuestionReplyParserTest` passes. It should cover:
  - each period type
  - unsupported
  - a reply in code fences, or with text around it
  - numbers written as strings
  - a null category
  - a reply with no JSON object, which throws `UnreadableReplyException`
- [x] 4.3 Add `LiteRtSpendingQuestionRepository` and bind it in `AiDataModule`:
  - Each question gets a new conversation, with greedy sampling and `maxOutputToken = 64`.
  - Only the elapsed time is logged, never the question.

  Verify with `LiteRtSpendingQuestionRepositoryTest`, which uses the existing fake engine factory pattern: the conversation config is used and the reply is parsed. Then confirm that the receipt prompt, the receipt parser and their tests are unchanged (`git diff --stat` shows no change to `ReceiptPromptBuilder*` or `LiteRtReceiptExtractionRepository*`).

## 5. Ask feature (`:feature:ask`)

- [x] 5.1 Add `AskUiState`, `AskAnswerUiModel` and `SpendingAnswerUiMapper` (design decision 8). The mapper builds the period text, the category label or "All spending", the formatted totals, and the receipt-count plurals. Write the mapper tests first:
  - a single month gives "1–31 Oct 2026"
  - a quarter, a year, a range across years, and all time
  - two currencies
  - nothing spent, and unanswerable

  Verify that `SpendingAnswerUiMapperTest` passes.
- [x] 5.2 Add `AskViewModel`, writing the tests first:
  - Question text is kept in `SavedStateHandle`.
  - `onShown()` and `onHidden()` start and cancel the model hold.
  - `ask()` sets `isAnswering`, then sets the answer.
  - On a model failure it sets the error and keeps the question.
  - Asking is ignored while the question is blank or already being answered.
  - An example fills the field.

  Verify that `AskViewModelTest` passes, written to the viewmodel conventions.
- [x] 5.3 Replace the placeholder `AskScreen` with these:
  - the question field, limited to 200 characters
  - the Ask button
  - example chips
  - the progress indicator
  - answer, nothing-spent, can't-answer and error content
  - `LifecycleStartEffect` calling `onShown()` and `onHidden()`

  Add previews. Verify with Compose tests in `:feature:ask` using `createComposeRule()`:
  - Ask is disabled while the field is blank.
  - Tapping an example fills the field.
  - An answer shows the category, the period, the amount and the count.
  - The can't-answer state shows the examples.

## 6. Navigation, docs and checks

- [x] 6.1 Update the `:app` instrumented tests that expect the Ask placeholder (`PocketReceiptsAppTest`, `ReceiptsFlowTest`) to expect the question field instead. They never load the model, because `FakeModelRepository.modelPath()` is `null`: keeping the model ready then returns at once, and reading a question fails before reaching the engine. So no fake `SpendingQuestionRepository` is needed.
- [x] 6.2 Update `.claude/CLAUDE.md` (the on-device AI note: Ask holds the engine while shown) and the README (features list, the "Ask is next" line, and the modules diagram if it changes). Verify that the text matches the spec deltas.
- [x] 6.3 Run `./gradlew check`, then the connected tests for `:feature:ask`, `:feature:receipts`, `:feature:capture` and `:data:receipts` on the emulator (`ANDROID_SERIAL=emulator-5554`). Verify that all pass.

## 7. Real phone

- [x] 7.1 On the owner's phone (`ANDROID_SERIAL=b6fff6b1`), with a few receipts saved across categories, months and currencies, the owner asks:
  - "How much did I spend on groceries in October?"
  - "How much is total spend in 2025?"
  - "How much in shopping this quarter?"
  - "How much did I spend last month?"
  - an unsupported question, such as "Which shop did I visit most?"

  Verify that each answer's category, dates and totals match the receipts. Record the time from tapping Ask to the answer, cold (just after opening Ask) and warm, and record the results in the PR.
  Done on 2026-11-23. The owner installed the build on the phone, asked the questions, and confirmed that the answers were correct. The phone's log no longer held the "Question read in" lines, so the times weren't recorded.
- [x] 7.2 After 7.1, run the `:app` connected tests on the emulator (they uninstall the app and delete the model). Verify that they pass.
  The `:app` suite last ran (passing) after all the app code was in place. The only later changes were to `AskScreenTest` and the docs, neither of which is part of the `:app` test APK.

## Workflow follow-up

- Open a PR from `feature/ask-spending` into `main`, and merge only after the owner reviews and approves.
- Archive the change with `/opsx:archive` after it is merged.
