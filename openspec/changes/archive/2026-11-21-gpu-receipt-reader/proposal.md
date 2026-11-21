# Proposal

## Why

On the owner's phone (OnePlus, Snapdragon 8+ Gen 1, 16 GB), Gemma takes 12–15 s to read a receipt on the CPU, which is almost the whole wait after the scanner. A test build that ran Gemma on the GPU read the same receipt in 4–7 s, with the same values:

| Scanner's last step → review form | CPU | GPU |
|---|---|---|
| Model already loaded | about 13 s | about 5 s |
| Model loaded fresh during the scan | about 16 s | about 9.5 s |

Loading is slower on the GPU (13.6 s against 4.6 s; 25 s the very first time, while LiteRT-LM prepares and caches its GPU programs), but it overlaps the scan, so every scan is still faster. The gemma-receipt-extraction change chose CPU only because, on phones without OpenCL (the emulator, Pixels), LiteRT-LM's GPU backend falls back to WebGPU and ran for about 15 minutes before failing. Detecting OpenCL first avoids that path.

## What Changes

- Run Gemma on the GPU when the phone has an OpenCL library, and on the CPU otherwise.
- If the GPU engine can't be created, or fails while reading a receipt, use the CPU for the rest of the app's run, and read that receipt again on the CPU, so the user still gets a result.
- Declare the GPU libraries LiteRT-LM needs (`libOpenCL.so`, `libvndksupport.so`, both optional) in `:data:ai`'s manifest.
- Setup's load test uses the same choice, so a phone whose GPU fails but whose CPU works can still use the app, and the slow first-ever GPU load happens during setup.
- Log which processor the model loaded on (debug).
- Not changed: the model file, the prompt, the reply limit, sampling, when the model loads and when it's released.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `receipt-capture`: adds a requirement that receipts are read on the graphics processor where the phone supports it, with the CPU as fallback.
- `model-setup`: "Phone can't run the model" applies only when the model can't be loaded on either processor.

## Impact

- `:data:ai`: `LiteRtEngineFactory` picks the backend and falls back; `LiteRtModelRuntime` reloads on the CPU after a GPU failure during reading; a small `GpuSupport` check; a new `src/main/AndroidManifest.xml` with the two `uses-native-library` entries.
- No change to `:domain`, the features, privacy rules, permissions or the model download.
- Memory: the GPU backend may use more memory than the CPU; measured in the verification task.
- Docs: README (Scan receipts) and the on-device AI note in `.claude/CLAUDE.md`.
