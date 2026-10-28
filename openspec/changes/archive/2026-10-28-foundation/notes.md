# Implementation notes

## Task 7.5: manual device checks (2026-10-28)

Device: `Pixel_10` emulator, API 37, debug build.

| Check | Result |
|---|---|
| Edge-to-edge, gesture navigation | Pass: navigation bar sits above the gesture handle; all items tappable; heading clear of the status bar. |
| Edge-to-edge, three-button navigation | Pass: system buttons drawn below the app's navigation bar; nothing covered. |
| Back from Ask (system back) | Pass: Receipts shown and selected. |
| Back from Receipts | Pass: the app closes to the launcher. |
| Predictive back preview from Ask | Pass: during a partial back swipe, the Receipts heading is previewed behind Ask. The selected tab indicator updates only once back completes. |
| Process recreation | Pass: with Ask open, app sent home and its process killed (`kill -9` as the app user; `am kill` didn't stop the process on this image). Relaunching from the launcher started a new process showing Ask, selected. |

## Deviations from tasks

- **Task 2.5:** the debug APK doesn't package `META-INF/*.kotlin_module` files at all. The check was made on the module outputs instead: `feature-receipts_debug.kotlin_module` and `data-receipts_debug.kotlin_module` are distinct.
- **Task 6.2:** the downloaded Material Symbols set `android:tint="?attr/colorControlNormal"`, an AppCompat theme attribute this app doesn't have. The attribute was removed; Compose's `Icon` applies the tint.
- **Task 7.3:** instrumented test names use underscores (`test_that_...`), not backticks with spaces. Method names with spaces need DEX format 040 (API 30+), and minSdk is 26.
