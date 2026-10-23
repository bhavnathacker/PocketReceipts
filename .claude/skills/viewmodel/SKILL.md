---
name: viewmodel
description: >
  Create, update, and refactor Android ViewModels using Test-Driven Development (TDD).
  Enforces writing test cases before production code, follows project ViewModel conventions
  (Hilt, private MutableStateFlow exposed as StateFlow, use case injection), and supports three modes:
  create, update, and refactor.
triggers:
  - /viewmodel
  - create viewmodel
  - new viewmodel
  - update viewmodel
  - refactor viewmodel
---

# ViewModel TDD Skill

Create, update, and refactor Android ViewModels using strict Test-Driven Development.

## Usage

```
/viewmodel create MyFeatureViewModel --module feature/myfeature
/viewmodel create MyFeatureViewModel --module feature/myfeature --state-type sealed
/viewmodel create MyFeatureViewModel --module feature/myfeature --assisted
/viewmodel update MyFeatureViewModel --module feature/myfeature --change "add logout action"
/viewmodel refactor MyFeatureViewModel --module feature/myfeature
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `create\|update\|refactor` | Mode of operation (required, first positional arg) | `create` |
| ViewModel name | Name of the ViewModel class (required, second positional arg) | `MyFeatureViewModel` |
| `--module <path>` | Module path relative to project root (required) | `--module feature/scan` |
| `--state-type <type>` | UI state structure: `flat` (default) or `sealed` | `--state-type sealed` |
| `--assisted` | Use assisted injection for runtime parameters | `--assisted` |
| `--change <desc>` | Description of what to change (for `update` mode) | `--change "add search filtering"` |

---

## Execution Steps

### Mode: CREATE

#### Step 1 — Gather Requirements

1. Confirm the ViewModel name, module path, and feature description with the user.
2. Search the target module for existing ViewModels and tests to match local package structure and conventions:
   ```
   Glob: <module>/src/main/**/presentation/**/*ViewModel.kt
   Glob: <module>/src/test/**/*ViewModelTest.kt
   ```
3. Determine the package name by following existing module conventions.
4. Ask the user:
   - What data/state does this screen need to display?
   - What user actions does this screen handle?
   - What use cases / domain dependencies are needed?
   - Does this ViewModel need runtime parameters from navigation? (If yes, use assisted injection.)

#### Step 2 — Define Test Cases (User Confirmation Gate)

Based on the gathered requirements, propose a list of test cases. Present them to the user in this format:

```
Proposed test cases for MyFeatureViewModel:

1. `test that initial state is loading`
2. `test that state contains items when use case emits successfully`
3. `test that error in flow is caught and logged`
4. `test that onAction calls the correct use case`
...

Please confirm, modify, or add test cases before proceeding.
```

**Do NOT proceed until the user confirms the test cases.**

#### Step 3 — Generate Test File

Create the test file at `<module>/src/test/kotlin/<package>/<Name>ViewModelTest.kt` following the Test Standards below. Include all confirmed test cases with proper structure but **failing implementations** (the tests should compile but fail — this is the RED phase).

#### Step 4 — Verify RED

Run the tests to confirm they fail:
```bash
./gradlew <module-gradle-path>:testDebugUnitTest --tests "<fully.qualified.TestClass>"
```

Report the results to the user. All new tests should fail (RED).

#### Step 5 — Generate Production Code

Create the following files:

1. **UI State** at `<module>/src/main/kotlin/<package>/model/<Name>UiState.kt`
2. **Mappers** (if needed) at `<module>/src/main/kotlin/<package>/mapper/<Name>Mapper.kt`
3. **ViewModel** at `<module>/src/main/kotlin/<package>/<Name>ViewModel.kt`

Follow the ViewModel Standards below for all generated code.

#### Step 6 — Verify GREEN

Run the tests again:
```bash
./gradlew <module-gradle-path>:testDebugUnitTest --tests "<fully.qualified.TestClass>"
```

All tests should now pass (GREEN). If any test fails, fix the production code and re-run until green.

#### Step 7 — Refactor (if needed)

Review the generated code for any improvements while keeping tests green. Run tests after any refactoring.

---

### Mode: UPDATE

#### Step 1 — Locate Files

Find the existing ViewModel, its test, and UI state:
```
Glob: **/<Name>ViewModel.kt
Glob: **/<Name>ViewModelTest.kt
Glob: **/<Name>UiState.kt
```

Read all located files to understand the current implementation.


#### Step 2 — Define New Test Cases (User Confirmation Gate)

Based on the `--change` description, propose new test cases. Present them to the user for confirmation.

**Do NOT proceed until the user confirms.**

#### Step 3 — Add Tests (RED)

Add the new test cases to the existing test file. Run tests to verify the new tests fail while existing tests still pass.

#### Step 4 — Implement Change (GREEN)

Modify the ViewModel (and UI state / mappers if needed) to make the new tests pass. Do not break existing tests.

#### Step 5 — Verify GREEN

Run the full test suite for the ViewModel to ensure all tests pass (both old and new).

---

### Mode: REFACTOR

#### Step 1 — Analyze

Locate and read the ViewModel, test file, and UI state. Identify:
- Direct repository/data layer dependencies that should be replaced with use cases
- Missing error handling patterns
- Any other deviations from the ViewModel Standards below

Present the analysis to the user.

#### Step 2 — Ensure Test Coverage

Check that existing tests cover the current behavior. If coverage is insufficient, propose additional test cases and get user confirmation before adding them. All existing tests must pass before refactoring begins.

#### Step 3 — Plan Refactoring

Present a step-by-step refactoring plan to the user. Each step should be a small, verifiable change.

#### Step 4 — Apply Incrementally

For each refactoring step:
1. Make the change
2. Run tests to verify nothing broke
3. If tests fail, fix or revert before proceeding

---

## ViewModel Standards

See [viewmodel-conventions.md](./viewmodel-conventions.md) for all ViewModel production code conventions.

---

## Test Standards

See [viewmodel-test-conventions.md](./viewmodel-test-conventions.md) for all ViewModel test conventions.

---

## Guidelines

1. **TDD is mandatory** — tests are always written before production code. Never generate ViewModel code before the test file exists and fails.
2. **User confirmation gate** — always present proposed test cases and wait for explicit user approval before writing any test or production code.
3. **Module convention matching** — before generating code, search the target module for existing ViewModels and tests. Match the local package structure, naming patterns, and any module-specific conventions.
4. **Single state pattern** — back the state with a private `MutableStateFlow`, expose it as `StateFlow` via `asStateFlow()`, and change it only with `_uiState.update { it.copy(...) }`. One-shot events are `StateEventWithContent` fields in the UI state.
5. **Incremental verification** — run tests after every significant code change. Never assume code is correct without verification.
6. **Minimal changes** — when updating or refactoring, change only what is necessary. Do not refactor unrelated code.
