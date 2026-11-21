# Design

## Context

- `LiteRtEngineFactory.create(modelPath)` (`:data:ai`) creates an `Engine` with `Backend.CPU()` and `cacheDir = context.cacheDir`, initializes it, and closes it if `initialize()` throws.
- `LiteRtModelRuntime` owns the one shared engine (holders, 2-minute idle release, release on background). `withEngine` runs reading and setup's load test; `hold` keeps the engine loaded while the scanner is open. All engine work runs on the single `@InferenceDispatcher` thread, with state under a `Mutex`.
- gemma-receipt-extraction (design decision 5) chose CPU only: on the emulator, which has no OpenCL, the GPU backend fell back to WebGPU, ran for about 15 minutes and failed. Pixels have no OpenCL either. It left "revisit the GPU backend once it can be detected as fast and reliable on a given phone" open.
- The spike on the owner's phone (Snapdragon 8+ Gen 1, `/vendor/lib64/libOpenCL.so` present) with `Backend.GPU()` and the two `uses-native-library` entries: LiteRT-LM used OpenCL ("Created OpenCL device"), load 25 s the first time and 13.6 s after, reading 4–7 s (CPU: 12–15 s), correct values. The emulator has no `libOpenCL.so` in `/vendor/lib64`, `/system/vendor/lib64` or `/system/lib64`.
- Without the manifest entries, a GPU engine initializes but the first reply fails with "Can not find OpenCL library on this device" (LiteRT-LM guidance).

## Goals / Non-Goals

**Goals:**
- GPU on phones with OpenCL; CPU everywhere else, with no attempt at the WebGPU path.
- A GPU failure, at load or while reading, never costs the user a receipt.

**Non-Goals:**
- NPU backends, or a per-phone allow or deny list.
- Remembering a GPU failure across app restarts.
- Changing the prompt, reply limit, sampling, model file, or the loading and release timing.

## Decisions

### 1. OpenCL detection by loading the library

New internal `interface GpuSupport { val isAvailable: Boolean }`, bound to `OpenClGpuSupport`. It calls `System.loadLibrary("OpenCL")` once (lazily, on the inference thread) and reports `true` if it loads, `false` on `UnsatisfiedLinkError` or `SecurityException`. With the `uses-native-library` declaration, Android 12+ lets the app load the vendor's `libOpenCL.so`; on Android 8–11, OpenCL is loadable where the vendor lists it as a public library, which is also what LiteRT-LM itself needs.

**Why loading and not checking file paths:** loading is exactly what the GPU backend will do, and it respects the linker namespace rules. A path check could find a library the app isn't allowed to load, or miss one in another directory.

**Alternative:** let LiteRT-LM try the GPU and fall back on failure. Rejected: without OpenCL it takes the WebGPU path, which ran for about 15 minutes on the emulator before failing.

### 2. Backend choice and load fallback in the runtime

- `EngineFactory.create(modelPath, backend)` takes a small internal `enum class ReaderBackend { GPU, CPU }` and maps it to `Backend.GPU()` or `Backend.CPU()`. It still closes the engine if `initialize()` throws.
- `LiteRtModelRuntime` keeps `gpuUsable`, initially `gpuSupport.isAvailable`, and the loaded engine's backend.
- Loading: if `gpuUsable`, create on GPU. If that throws (`Exception` or `OutOfMemoryError`), log it, set `gpuUsable = false`, and create on CPU. A CPU failure propagates as now.
- Log "Model loaded on %s in %s" (debug).

**Why in the runtime, not the factory:** the runtime already owns the engine's lifecycle and is the only place that can also handle failures during reading (decision 3), so both fallbacks share one `gpuUsable` flag.

### 3. Reading fallback

In `withEngine`, if the block throws an `Exception` (not `CancellationException`) while the engine is on the GPU: log it, set `gpuUsable = false`, close the GPU engine, load the CPU engine and run the block once more. A failure on the CPU propagates to the caller, which shows the existing ReadFailed choices.

- This runs under the same rules as before: on the inference thread, with the caller still counted as a holder, so a concurrent `hold` simply keeps the new CPU engine loaded.
- `LiteRtReceiptExtractionRepository` doesn't change: its block opens a new conversation each time, so the retry starts clean.

**Why retry in the runtime:** the user scanned once and should get one result; the repository shouldn't know about backends.

### 4. Failure memory: in-process only

`gpuUsable` lives in the singleton runtime and resets when the process restarts. A phone whose GPU fails repeatably pays one failed GPU attempt per app run. Persisting it (for example in DataStore) would need a way to retry after a driver or app update; that's not worth it until a failure is actually seen.

### 5. Manifest entries in `:data:ai`

`data/ai/src/main/AndroidManifest.xml` declares `uses-native-library` for `libOpenCL.so` and `libvndksupport.so`, both `android:required="false"`, so phones without them can still install the app. The module that uses LiteRT-LM owns the declaration; manifest merging puts it in the app. No permission is involved, so `verifyInternetPermissionSources` and `verifyNoUsageLoggingBackend` are unaffected.

### 6. Setup's load test

`canLoad` goes through `withEngine`, so it uses the same choice and load fallback (decision 2). On GPU phones it also pays the slow first-ever GPU load (about 25 s, kernels cached in `cacheDir`) during setup instead of on the first scan. It doesn't run a prompt, so a GPU that loads but fails at reading is caught by decision 3 at the first scan.

## Risks / Trade-offs

- [A GPU driver crashes natively instead of throwing] → Can't be caught in Kotlin; the app would crash on each scan on that phone. Mitigation: none in this change. The spike ran without problems on Adreno; if crashes appear, add a persisted "GPU failed" flag set before reading and cleared after.
- [Slower load on the GPU (13.6 s vs 4.6 s)] → It overlaps the scan; on the owner's phone fresh scans are still about 6 s faster overall. A user who finishes the scan within a few seconds waits for the load, but would have waited longer for the CPU read.
- [GPU memory use] → Measured on the owner's phone in task 4.2 (`dumpsys meminfo` while loaded). The engine is still released 2 minutes after the last scan or when the app goes to the background.
- [`cacheDir` cleared by the system] → The next GPU load prepares its programs again (about 25 s once).
- [`System.loadLibrary("OpenCL")` loads a vendor library into the process] → LiteRT-LM loads the same library on the GPU path anyway; on phones without it nothing is loaded.

## Migration Plan

No data or settings migration. Rollback is reverting the change; the app then runs on the CPU everywhere again.
