# Design

## Context

- `LiteRtModelRuntime.withEngine` (`:data:ai`) creates an `Engine` on the CPU backend, calls `initialize()` (which reads the 2.6 GB model), runs one block and closes the engine. Both `canLoad` (the setup load test) and `LiteRtReceiptExtractionRepository.extract` go through it. All engine work runs on the single-thread `@InferenceDispatcher`.
- The gemma-receipt-extraction design (decision 5) chose "load per scan" and named "a long-lived engine with an idle timeout" as the alternative to revisit. This change takes that alternative, scoped to scanning.
- The capture destination (`CaptureKey`, `CaptureViewModel`) exists from the tap on "Scan receipt" until the scan has been read (the app then calls `backStack.replaceTop(receiptReviewKey(scan))`) or the user cancels. Its ViewModel is cleared at that point.
- The ML Kit document scanner is a Google Play services activity. While it's in front, every PocketReceipts activity is stopped, so the process counts as in the background (`ProcessLifecycleOwner` reaches `ON_STOP`).
- There's no app-wide `CoroutineScope` yet; `:data:model-download` creates its own flows on the IO dispatcher.

## Goals / Non-Goals

**Goals:**
- The model load overlaps the time the user spends in the scanner.
- A second scan within 2 minutes reuses the loaded engine.
- Exactly one engine exists at a time, and its memory is released when no scan needs it.

**Non-Goals:**
- Faster prompts or replies (prompt, `maxOutputToken = 256`, sampling stay as they are; the owner chose not to change them).
- GPU or NPU backends.
- Warming the model anywhere other than the capture flow (for example, on app start or for Ask).
- Showing load progress in the UI.

## Decisions

### 1. One shared engine with holders, an idle timeout and a background release

`LiteRtModelRuntime` becomes the owner of at most one `Engine`:

- **State** (guarded by a `Mutex`, all engine calls on `@InferenceDispatcher`): the loaded engine and its model path, the number of active holders, and a pending release job.
- **`withEngine(modelPath, block)`:** uses the loaded engine, loading it first if needed (or waiting for the load in progress, because loads and blocks run in order on the single inference thread). The engine is not closed after the block. Counts as a holder while the block runs.
- **`hold(modelPath)`:** loads the engine if needed, then suspends until cancelled, counting as a holder throughout. This is what the capture flow uses.
- **Release rules**, checked whenever the holder count drops to zero or the app's state changes:
  - holder count is zero and the app is in the background → close now;
  - holder count is zero and the app is in the foreground → close after 2 minutes, unless a holder arrives first (the pending job is cancelled).
  - Holders always win: while any holder is active, nothing closes the engine, whatever the app state. That's what keeps the model loaded while the scanner (another app's activity) is in front.
- **Load failure:** the exception propagates to the caller. Nothing is cached, so the next caller tries again.
- **`canLoad`** (setup) keeps its meaning: it calls `withEngine(path) { true }`, then asks the runtime to close right away (`releaseNow()`), because setup shouldn't leave 1–2 GB loaded on the setup screen.

**Why holders and not just a timeout:** a timeout alone would release the model while a user spends more than 2 minutes in the scanner, and "release on background" alone would release it the moment the scanner opens. Holders tie the model's lifetime to the scan flow, and the timeout only covers the gap between scans.

**Alternatives:**
- Keep the engine for the whole time the app is open: rejected by the owner (memory held while browsing).
- `onTrimMemory`-based release only: the levels a stopped app receives don't tell "scanner in front" apart from "user left the app", and some levels are deprecated on recent Android versions. Process lifecycle plus holders is deterministic and testable.

### 2. App foreground/background from `ProcessLifecycleOwner`

`:data:ai` adds `androidx.lifecycle:lifecycle-process` (same version as the existing lifecycle libraries, `lifecycleRuntimeKtx`). A small internal `AppVisibility` interface (`val isInForeground: StateFlow<Boolean>`), implemented with `ProcessLifecycleOwner.get().lifecycle.currentStateFlow` mapped to `isAtLeast(STARTED)`, is injected into the runtime so tests can drive it with a `MutableStateFlow`. The runtime collects it in its own scope (`SupervisorJob()` + `@InferenceDispatcher`), created by the Hilt singleton.

### 3. Domain contract: a hold, not a "warm up" call

- `ModelRuntimeRepository` gains `suspend fun keepLoaded(modelPath: String)`, documented as: loads the model if needed, keeps it loaded until the caller's coroutine is cancelled, and throws if loading fails.
- New `KeepReceiptReaderReadyUseCase` (`:domain`, `domain.scan` package): gets `ModelRepository.modelPath()`, returns quietly when it's null, and otherwise calls `keepLoaded`. It catches loading exceptions (not `CancellationException`) and `OutOfMemoryError`, then returns; the failure is logged by the data layer, and reading will try again and report it through the existing ReadFailed path.
- `LiteRtModelRuntimeRepository.keepLoaded` delegates to `runtime.hold(modelPath)`.

**Why a suspending hold:** structured concurrency releases the holder automatically when its scope ends, so no "release" call can be forgotten. A `prepare()`/`release()` pair would need explicit bookkeeping in the ViewModel.

### 4. Capture flow keeps the reader ready

`CaptureViewModel` injects `KeepReceiptReaderReadyUseCase` and launches it in `viewModelScope` from `init`. The ViewModel is created when the capture destination opens, before the scanner is launched, and cleared when the review form replaces it or the user cancels, which cancels the hold and starts the 2-minute timer (or releases at once if the app is in the background). Reading (`onImageScanned`) is unchanged: `ScanReceiptUseCase` → `extract` → `withEngine` finds the engine loaded, or queues behind the load in progress.

### 5. Fresh conversation per receipt

`LiteRtReceiptExtractionRepository` keeps creating a new `Conversation` per `extract` and closing it with `use`. The engine holds no conversation state between receipts, which is what "Receipts read independently" requires. No code change, but a unit test pins it (two extractions → two conversations, each closed).

### 6. Measuring the gain

`LiteRtModelRuntime` logs how long `initialize()` took, and `LiteRtReceiptExtractionRepository` logs how long `sendMessage` took, with Timber at debug level (Timber is only planted in debug builds). The real-model check compares the time from tapping the scanner's Done to the review form with the earlier result (Bharat Medical, upright: 53 s on the emulator).

## Risks / Trade-offs

- [The model's 1–2 GB stays resident while the scanner is open and for up to 2 minutes after] → Released at once when the app goes to the background without a scan in progress; Android may still kill the process under memory pressure, which loses nothing (the next scan loads again).
- [Process killed while the scanner is open] → The scan result returns to a recreated `CaptureViewModel`, which starts a new hold; reading then waits for that load, the same cost as today.
- [`engine.initialize()` is a blocking native call that can't be interrupted] → A cancelled hold doesn't stop a load in progress; the release rules run once it finishes. Reading queued behind it gets the loaded engine.
- [Closing the engine while a block is running] → Impossible by construction: a running `withEngine` block counts as a holder, and closing only happens with zero holders, under the mutex, on the inference thread.
- [Tests depend on time] → The 2-minute timeout runs on the injected dispatcher, so runtime tests use a `StandardTestDispatcher` and `advanceTimeBy`. The `Engine` itself is created through a small factory interface so tests don't need native code.

## Migration Plan

No data or settings migration. Rollback is reverting the change; the engine then loads per scan again.
