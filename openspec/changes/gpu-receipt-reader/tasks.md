# Tasks

## 1. GPU support (`:data:ai`)

- [x] 1.1 Add `data/ai/src/main/AndroidManifest.xml` with `uses-native-library` for `libOpenCL.so` and `libvndksupport.so`, both `android:required="false"`. Verify `./gradlew :app:processDebugMainManifest` succeeds and the merged manifest contains both entries, and `./gradlew :app:verifyInternetPermissionSources :app:verifyNoUsageLoggingBackend` pass.
- [x] 1.2 Add the internal `GpuSupport` / `OpenClGpuSupport` (design decision 1) and `ReaderBackend`, and bind `GpuSupport` in `AiDataModule`. Change `EngineFactory.create` to take a `ReaderBackend` and map it to `Backend.GPU()` / `Backend.CPU()`, still closing the engine when `initialize()` throws. Verify `./gradlew :data:ai:assembleDebug` succeeds.

## 2. Backend choice and fallbacks (`LiteRtModelRuntime`)

- [x] 2.1 Load on the GPU when `gpuUsable`, falling back to the CPU when the GPU load throws an `Exception` or `OutOfMemoryError`; log the backend and load time (design decision 2). Verify with `LiteRtModelRuntimeTest`, using a fake factory that records backends and a fake `GpuSupport`: GPU available → loads on GPU; not available → loads on CPU without trying GPU; GPU load fails → loads on CPU, and later loads (after a release) go straight to CPU; both fail → the exception reaches the caller and nothing is cached.
- [x] 2.2 In `withEngine`, when the block throws an `Exception` on a GPU engine, close it, set `gpuUsable = false`, load on the CPU and run the block once more (design decision 3). Verify with tests: a block failing on GPU is retried on CPU and its result returned; a block failing on CPU isn't retried; a block failing on the CPU retry propagates; `CancellationException` isn't retried; an active `hold` keeps the CPU engine loaded afterwards; all existing runtime and repository tests still pass.

## 3. Documentation and checks

- [x] 3.1 Update the KDoc of `LiteRtEngineFactory` and `LiteRtModelRuntime`, the on-device AI note in `.claude/CLAUDE.md` (GPU where OpenCL loads, CPU otherwise, fallbacks), and the README's Scan receipts text if it mentions the processor. Verify the text matches the spec deltas.
- [x] 3.2 Run `./gradlew check` and the connected tests for `:feature:capture` on the emulator (`ANDROID_SERIAL=emulator-5554`). Verify all pass.

## 4. Real devices

- [x] 4.1 On the emulator (no OpenCL), install the app with the model present and open the scanner. Verify from the debug log that the model loads on the CPU, with no GPU or WebGPU attempt, in about the same time as before.
- [x] 4.2 On the owner's phone (`ANDROID_SERIAL=b6fff6b1`, the owner scans), scan the same receipt twice in a row, then once more after the model has been released. Verify from the debug log that the model loads on the GPU, record the load and read times against the CPU numbers in the proposal, check the values are correct, and record the app's memory while the model is loaded (`dumpsys meminfo com.bhavnathacker.pocketreceipts`). Record the results in the PR.
- [x] 4.3 After 4.1 and 4.2, run the `:app` connected tests on the emulator (they uninstall the app and delete the model). Verify they pass.

## Workflow follow-up

- Open a PR from `feature/gpu-receipt-reader` into `main`; merge only after the owner reviews and approves.
- Archive the change with `/opsx:archive` after it is merged.
