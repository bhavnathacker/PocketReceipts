# Proposal

## Why

Pocket Receipts is a private, on-device expense tracker: scan a receipt, on-device AI reads it, and the user asks questions about their spending in plain language. Nothing about the user's spending leaves the phone. Today the project is a single `:app` module plus `:core:test`, with no DI, no module boundaries and no build conventions. Every later MVP change (receipts, capture, model setup, AI extraction, ask) needs the module structure, Hilt and build rules in place first. The privacy guarantee also has to be enforced by the build from day one, not added later.

### MVP vision (context for this and later changes)

- **Capture**: ML Kit Document Scanner, then ML Kit OCR. Code parses date/amount candidates; Gemma 4 E2B (LiteRT-LM, text-only) picks candidate IDs plus merchant and category; a validator flags uncertain fields; the user confirms on a review form.
- **Ask**: the LLM turns a question into a typed `QuerySpec`; code resolves periods, runs fixed Room queries and formats the answer from a template. The LLM never produces a number the user sees.
- **Model**: a one-time, resumable, checksum-verified download from Hugging Face (pinned revision). The app stays usable without it (manual entry, drafts).
- **Privacy**: the network is used only for the model download; no analytics; no cloud backup.

### MVP roadmap (one OpenSpec change each)

1. **foundation** (this change)
2. receipts-manual: data model, Room (and its convention plugin), list/detail/edit/delete, manual entry
3. capture-ocr: Document Scanner, OCR, candidate parsers, review form
4. model-setup: onboarding, background download, model settings
5. ai-extraction: LiteRT-LM gateway, field selection, validation flags, eval set
6. ask: QuerySpec, PeriodResolver, Ask screen
7. release-readiness: R8, backup rules, accessibility/screenshot pass, Play listing, Baseline Profile

## What Changes

- Add a `build-logic` included build with convention plugins (Android application, JVM library, Android library, Android feature, Hilt), all configuration-cache compatible.
- Create the module skeleton: `:domain` (pure Kotlin), `:core:ui`, `:core:ui-test`, `:feature:receipts`, `:feature:capture`, `:feature:ask`, `:feature:modelsetup`, `:feature:settings`, `:data:receipts`, `:data:ai`, `:data:model-download`. Each module uses a `<module>.gradle.kts` build file.
- Enforce dependency rules: features depend only on `:domain` and `:core:ui`; data modules depend only on `:domain`; only `:app` wires everything together.
- Add a build check that fails if any module other than `:data:model-download` depends on an HTTP client.
- Set up Hilt: `@HiltAndroidApp` on `PocketReceiptsApplication`, `@AndroidEntryPoint` on `MainActivity`, `@IoDispatcher`/`@DefaultDispatcher` qualifiers in `:domain` with a Hilt dispatcher module.
- **BREAKING**: raise `minSdk` from 24 to 26, so `java.time` works without desugaring and matches the Gemini Nano requirement for v1.x.
- Add the Navigation 3 app shell: top-level destinations (Receipts, Ask, Settings) shown as placeholders in a `NavigationSuiteScaffold`, edge-to-edge, with each feature exposing its `NavKey`s and entry provider.
- Add GitHub Actions CI: lint, JVM unit tests and coverage report on every push.
- Update `.claude/CLAUDE.md`: dependency direction (feature and data both depend on domain) and the file-structure example (no `data/` package inside feature modules).

## Capabilities

### New Capabilities
- `app-navigation`: top-level app shell, covering destinations, adaptive navigation (bar/rail), back behaviour and edge-to-edge layout.
- `privacy-guarantees`: build-enforced privacy rules. Network access is confined to the model-download module, and no analytics or crash-reporting SDKs are allowed. Later changes extend it (for example, backup exclusion in release-readiness).

### Modified Capabilities
- None (no existing specs).

## Impact

- **Build**: new `build-logic`, `settings.gradle.kts` module includes and build-file names, new version catalog entries (Android library plugin, Hilt, KSP, kotlinx.serialization, Material 3 adaptive navigation-suite, Kover). Room and its convention plugin arrive in `receipts-manual`, with the first entity.
- **Code**: `:app` gains Hilt, the Nav3 shell and placeholder screens; new empty-but-wired modules.
- **Devices**: Android 7.x (API 24–25) is no longer supported.
- **CI**: new `.github/workflows` pipeline.
- **Docs**: `.claude/CLAUDE.md` architecture section corrected.
- **Out of scope**: any receipt data, AI, capture or download functionality. Those come in later changes.
