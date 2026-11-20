# Proposal

## Why

Reading a scanned receipt takes 30–55 seconds on the emulator (Pixel 10 image, CPU). Part of every scan is loading the 2.6 GB Gemma model from scratch: `LiteRtModelRuntime.withEngine` creates an engine, reads the model, answers one prompt and closes the engine again. The user waits through that load after tapping Done, although the app knew a scan was coming as soon as they tapped "Scan receipt", and has to wait through it again for a second receipt scanned right after.

## What Changes

- Start loading Gemma when the scan screen opens, so it loads while the user photographs, crops and confirms the receipt in the document scanner.
- Keep the loaded model while the scan screen is open, including while the scanner (a Google Play services screen, during which the app counts as being in the background) is in front.
- After the scan screen closes, keep the model for 2 minutes so a second scan starts without loading. Release it after 2 minutes without a scan, or as soon as the app goes to the background with no scan screen open.
- Each scan still gets a fresh conversation, so one receipt's text never influences the next.
- A failed warm-up is not shown to the user: scanning works as today, and reading tries to load the model itself and reports a failure the way it does now.
- Not changed: the prompt, the reply limit, greedy sampling, the CPU backend and the setup load test.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `receipt-capture`: adds a requirement that the receipt reader is prepared while the user scans, kept briefly for another scan, and released when it's no longer needed.

## Impact

- `:domain`: `ModelRuntimeRepository` gains a way to keep the model loaded while a caller needs it. A new `KeepReceiptReaderReadyUseCase`.
- `:data:ai`: `LiteRtModelRuntime` keeps one shared engine instead of one per call, with an idle timeout and release when the app goes to the background. New dependency `androidx.lifecycle:lifecycle-process` (catalog version) for the app's foreground/background state.
- `:feature:capture`: `CaptureViewModel` keeps the reader ready for as long as it exists.
- Memory: the model's memory (1–2 GB) is now held for up to 2 minutes after a scan, and while the scanner is open, instead of only during reading.
- No change to privacy rules, manifests, the model download or the UI.
