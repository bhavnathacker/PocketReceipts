# Tasks

## 1. Dependency setup

- [x] 1.1 Add `androidx-lifecycle-process` (version ref `lifecycleRuntimeKtx`) to `gradle/libs.versions.toml` and as an `implementation` dependency of `:data:ai`. Verify `./gradlew :data:ai:assembleDebug` succeeds.

## 2. Domain contract

- [x] 2.1 Add `suspend fun keepLoaded(modelPath: String)` to `ModelRuntimeRepository`, with KDoc (loads if needed, keeps the model loaded until the caller is cancelled, throws if loading fails). Verify `./gradlew :domain:compileKotlin` succeeds.
- [x] 2.2 Add `KeepReceiptReaderReadyUseCase` in `domain.scan` (following usecase-conventions): returns when `ModelRepository.modelPath()` is null; otherwise calls `keepLoaded`; swallows loading exceptions and `OutOfMemoryError`, rethrows `CancellationException`. Verify with JUnit 5 tests: holds with the model path, returns without a path, returns after a loading failure and after OOM, and stops when cancelled.

## 3. Shared engine (`:data:ai`)

- [x] 3.1 Add an internal `EngineFactory` (creates and initializes an `Engine` on the CPU backend for a path) and an internal `AppVisibility` (`isInForeground: StateFlow<Boolean>`) with a `ProcessLifecycleOwner` implementation, and bind both in `AiDataModule`. Verify `./gradlew :data:ai:assembleDebug` succeeds.
- [x] 3.2 Rewrite `LiteRtModelRuntime` per design decision 1: one cached engine under a `Mutex` on the inference dispatcher; `withEngine` and `hold` count as holders; with zero holders it closes after 2 minutes in the foreground or at once in the background; holders block every release; a failed load caches nothing; `releaseNow()`; debug log of the load time. Verify with unit tests over a fake `EngineFactory`, a `MutableStateFlow` visibility and a `StandardTestDispatcher`: one load for hold + extract; extract during a load waits and doesn't load twice; release 2 minutes after the last holder; no release before then; a new holder cancels the pending release; background releases at once with no holder; background doesn't release while a holder is active (scanner in front) but releases when it ends; a failed load is retried by the next caller.
- [x] 3.3 Implement `LiteRtModelRuntimeRepository.keepLoaded` with `runtime.hold`, and make `canLoad` call `releaseNow()` after its load test. Verify with unit tests that `canLoad` still returns true/false as before and leaves no engine loaded.
- [x] 3.4 Log the time `sendMessage` takes in `LiteRtReceiptExtractionRepository` (debug). Verify with a unit test that two extractions on one loaded engine open two conversations and close each of them.

## 4. Capture flow (`:feature:capture`)

- [x] 4.1 Inject `KeepReceiptReaderReadyUseCase` into `CaptureViewModel` and launch it in `viewModelScope` from `init`. Verify with `CaptureViewModelTest`: the use case is called when the ViewModel is created, is cancelled when the ViewModel is cleared, and a failing use case leaves the scan flow unchanged (existing tests still pass).

## 5. Documentation and integration

- [x] 5.1 Update the KDoc of `LiteRtModelRuntime` and `CaptureViewModel`, the on-device AI note in `.claude/CLAUDE.md`, and the README's Gemma feature text (model loaded while scanning, kept for 2 minutes). Verify the text matches the receipt-capture spec delta.
- [x] 5.2 Run `./gradlew check` and the connected tests for `:feature:capture` and `:app` on the emulator (`ANDROID_SERIAL=emulator-5554`). Verify all pass.
- [ ] 5.3 On the emulator with the real model, scan the Bharat Medical Stores receipt twice in a row (photo supplied by the owner, not committed, deleted from the emulator afterwards). Record the time from the scanner's Done to the review form for each scan against the earlier 53 s, check from the debug logs that the second scan didn't load the model, and check that the logs show a release 2 minutes later. Record the results in the PR.

## Workflow follow-up

- Open a PR from `feature/warm-up-receipt-reader` into `main`; merge only after the owner reviews and approves.
- Archive the change with `/opsx:archive` after it is merged.
