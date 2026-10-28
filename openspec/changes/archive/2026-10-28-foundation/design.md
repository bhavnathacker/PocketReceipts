# Design

## Context

See proposal.md (Why) for motivation, and `specs/app-navigation` and `specs/privacy-guarantees` for the required behaviour.

Current state:
- Two Gradle projects: `:app` (Compose, Material 3, Timber, a "Hello Android" `MainActivity` with `enableEdgeToEdge()`, `PocketReceiptsTheme` in `app/.../ui/theme`) and `:core:test` (pure JVM: JUnit 5 API, coroutines-test, `CoroutineMainDispatcherExtension`).
- AGP 9.3.3 with built-in Kotlin (no `org.jetbrains.kotlin.android` plugin), Gradle 9.5, configuration cache on, daemon on JDK 25, Java 11 source/target, minSdk 24, compile/targetSdk 37.
- No `build-logic`, no Hilt, no CI. Navigation 3 artifacts are already in the version catalog but unused.
- `:core:test` already uses a custom build-file name (`test.gradle.kts`), the pattern every new module follows.

## Goals / Non-Goals

**Goals:**
- One place (convention plugins) that defines how each module type is built, so adding a module is a few lines.
- Make module dependency rules and privacy rules fail the build, not depend on review.
- A tab shell that later features plug into without touching each other.

**Non-Goals:**
- Room, WorkManager, kotlinx.serialization JSON parsing for AI output, or any feature logic (later changes).
- Instrumented tests in CI (later, with Gradle Managed Devices). This change's UI tests run locally.
- Multiple back stacks per tab (history is per app, not per tab, for now).
- Backup rules (release-readiness) and the R8 release setup.

## Decisions

### D1. Convention plugins in a `build-logic` included build
`build-logic/convention` holds precompiled Kotlin plugins (classes registered in `gradlePlugin {}`), included through `pluginManagement { includeBuild("build-logic") }`:

| Plugin id | Applies | Used by |
|---|---|---|
| `pocketreceipts.android.application` | AGP app, compose compiler, SDK levels, JUnit Platform | `:app` |
| `pocketreceipts.android.library` | AGP library, SDK levels, Java 11, JUnit Platform, standard unit-test dependencies | data modules, `:core:ui-test` |
| `pocketreceipts.android.compose` | compose compiler + BOM | `:core:ui`, features, `:app` |
| `pocketreceipts.android.feature` | library + compose + hilt + `:domain` + `:core:ui` + lifecycle/hilt-viewmodel/navigation3 deps | `:feature:*` |
| `pocketreceipts.jvm.library` | `kotlin("jvm")`, Java 11, JUnit Platform | `:domain`, `:core:test` |
| `pocketreceipts.hilt` | Hilt Gradle plugin, KSP, hilt-android + compiler | `:app`, features, data modules |

SDK levels (minSdk **26**, compile/target 37) live in a single Kotlin constant inside build-logic. Plugins read the version catalog through `VersionCatalogsExtension`, so versions stay in `libs.versions.toml`.

*Alternatives:* `buildSrc` invalidates the whole build cache on any change and is slower; copying blocks into 12 build files leads to drift. An included build is what Now in Android and Google's guidance use.

### D2. Module build-file names and Kotlin module names
`settings.gradle.kts` uses a small helper, `includeModule(":feature:receipts")`, that calls `include` and sets `buildFileName = "<last segment>.gradle.kts"`, keeping the CLAUDE.md naming rule in one place.

`:feature:receipts` and `:data:receipts` share a last segment. Two Kotlin modules with the same name can produce clashing `META-INF/*.kotlin_module` entries and confuse `internal` visibility. So the convention plugins set the Kotlin `moduleName` from the full project path (for example `feature-receipts`, `data-receipts`), and each Android namespace mirrors the package (`com.bhavnathacker.pocketreceipts.feature.receipts`).

### D3. Dependency direction enforced by plugin shape, not by a separate check
- `pocketreceipts.android.feature` adds `:domain` and `:core:ui` itself. Feature build files never list data or feature modules.
- Data modules use `pocketreceipts.android.library` and depend on `:domain` only.
- `:domain` is a JVM module, so it can't depend on Android code at all.
- A lightweight `verifyModuleDependencies` task (registered by the feature and library conventions) fails if a `:feature:*` project depends on a `:data:*` or another `:feature:*` project, or if a `:data:*` project depends on a `:feature:*` project. The check walks the declared project dependencies, using configuration-cache-safe providers.

*Alternative:* a third-party dependency-analysis plugin covers more but adds a heavy dependency and its own AGP 9 compatibility risk for a small rule set.

### D4. Privacy checks (spec: privacy-guarantees)
All three run as part of `check`. They are implemented in build-logic as cacheable tasks, with inputs declared as `Provider`/`FileCollection` so they work with the configuration cache.

1. **HTTP client confinement**: every module except `:data:model-download` gets `verifyNoHttpClient`. It reads the resolved **compile classpath** (`incoming.resolutionResult` as a provider) of the main variant and fails if any component matches the denylist of HTTP client groups (`com.squareup.okhttp3`, `com.squareup.retrofit2`, `io.ktor`, `com.android.volley`, `org.apache.httpcomponents`, `io.grpc`, `com.github.kittinunf.fuel`). The compile classpath, not the runtime one, is checked because `:app` legitimately gets OkHttp at runtime through `:data:model-download`'s `implementation` dependency. Only an `api` leak or a direct dependency puts it on another module's compile classpath, which is exactly the spec's leak scenario.
2. **No analytics / crash SDKs**: `:app` gets `verifyNoTrackingSdks`, which checks the **runtime** classpaths of the debug and release variants (they include every module) against a denylist: Firebase Analytics/Crashlytics/Performance, `play-services-measurement*`, `play-services-ads*`, Sentry, Bugsnag, AppsFlyer, Amplitude, Mixpanel, Segment, Facebook SDK, Adjust, Branch.
3. **INTERNET permission confinement**: `:app` gets `verifyInternetPermissionSources`. Its inputs are the app's own manifest plus every dependency's manifest, collected through an artifact view on the runtime classpath requesting the `android-manifest` artifact type, each paired with its component identifier. It fails if any source other than project `:data:model-download` declares `android.permission.INTERNET`, and the error names that source.

Denylists and the allowed project path are constants in one Kotlin file, unit-tested (D9).

### D5. Hilt and dispatchers
- `@HiltAndroidApp` on the existing `PocketReceiptsApplication` (keeps the Timber setup). `@AndroidEntryPoint` on `MainActivity`.
- `@IoDispatcher` and `@DefaultDispatcher` qualifiers in `:domain` (`...domain.dispatcher`), using `javax.inject.Qualifier`. `:domain` depends only on `javax.inject` and coroutines, never Hilt.
- `DispatcherModule` (`@InstallIn(SingletonComponent::class)`) lives in `:app`, the composition root, and provides `Dispatchers.IO` / `Dispatchers.Default`.
- KSP for Hilt's annotation processing, since kapt is in maintenance mode and slow.

### D6. Navigation shell (spec: app-navigation)
- Each feature exposes a public `@Serializable data object <Feature>Key : NavKey` and a public `EntryProviderScope<NavKey>.<feature>Entries(...)` extension. Navigation lambdas are parameters, so features never reference each other. Receipts, Ask and Settings provide placeholder screens. Capture and model-setup expose nothing yet.
- `:app` owns a `TopLevelDestination` enum (key, icon, label resource) and the back stack (`rememberNavBackStack(ReceiptsKey)`, saveable, so it survives configuration changes and process death).
- Tab switching is a plain Kotlin `TopLevelNavigator` acting on the back stack list, kept separate from Compose so it can be unit-tested:
  - select Receipts → back stack becomes `[Receipts]`
  - select another tab X → back stack becomes `[Receipts, X]` (reselecting X changes nothing)
  - the selected tab is the top-level key nearest the top of the stack
- This gives the spec's back behaviour for free: back pops to `[Receipts]`, then the system finishes the activity. Predictive back comes from `NavDisplay` (targetSdk 37 enables predictive back by default).
- `NavigationSuiteScaffold` (`material3-adaptive-navigation-suite`) wraps `NavDisplay`. An explicit `layoutType` comes from `currentWindowAdaptiveInfo()`: compact width → `NavigationBar`, otherwise → `NavigationRail`. That matches the spec exactly, not whatever the library's default is.
- Edge-to-edge stays (`enableEdgeToEdge()`). The scaffold consumes insets for the bar/rail, and placeholder screens apply the remaining content insets.
- Icons are Material Symbols vector drawables committed to `:core:ui` resources, not `material-icons-extended`, which is large and no longer updated.

*Alternative considered:* a separate back stack per tab (Nav3 multiple-back-stacks recipe). It's better once tabs have deep screens, but it isn't needed while tabs are flat, and it would change the back behaviour the spec defines. Revisit in a later change.

### D7. `:core:ui`
`PocketReceiptsTheme` (`Color.kt`, `Theme.kt`, `Type.kt`) moves from `:app` to `:core:ui` (`...core.ui.theme`) unchanged, keeping dynamic color. Shared icons go here too. `:core:ui-test` exposes `ui-test-junit4` as `api` for instrumented tests and holds future shared test helpers.

### D8. CI
`.github/workflows/ci.yml` runs on push to `main` and on pull requests: checkout → `actions/setup-java` (JDK 25, matching the daemon toolchain) → `gradle/actions/setup-gradle` (caching, configuration cache) → `./gradlew check koverXmlReport` → upload the lint and Kover reports. `check` covers lint, JVM unit tests and all verify tasks from D3/D4. Kover is applied in the root project with every module as a `kover` dependency, giving one aggregated report.

### D9. Testing approach for this change (TDD order)
- **JVM unit tests (JUnit 5)**: `TopLevelNavigator` (all selection and reselection cases), and the build-logic rule logic as pure functions (`given component coordinates + project path → violations`), tested in `build-logic` with JUnit 5. These tests are written first.
- **Gradle TestKit functional tests** in build-logic for the HTTP-client rule, using small JVM fixture projects: a violating module fails, the allowed module passes, an `api` leak fails. AGP fixtures would need an SDK in TestKit, so the manifest rule is checked by a manual negative check during implementation (listed in tasks) instead.
- **Compose UI tests (instrumented, JUnit 4)** in `:app`: launch on Receipts, switching, reselecting, back from Ask returns to Receipts, rotation restores the tab (`StateRestorationTester`), and bar vs rail at forced widths (`DeviceConfigurationOverride.ForcedSize`).

## Risks / Trade-offs

- [Hilt and KSP with AGP 9's built-in Kotlin are a newer combination] → Pick the newest stable Hilt/KSP releases that state AGP 9 built-in Kotlin support; this is the first task, so problems surface before anything depends on them.
- [Kover with AGP 9 may lag behind] → If it fails, keep CI green without coverage and record the gap; coverage is a nice-to-have in this change.
- [The `android-manifest` artifact type is an AGP convention, not a documented public API] → The check is isolated in one task; if a future AGP breaks it, the build fails loudly (better than passing silently). The fallback is parsing the merged-manifest blame output.
- [Future third-party libraries (e.g. ML Kit / Play services) may declare INTERNET or include usage-logging transports] → The INTERNET check will flag them by design. The capture-ocr and ai-extraction changes must decide explicitly: remove the permission with `tools:node="remove"` where it isn't needed, and confirm whether those libraries send usage telemetry and how to turn it off. That's a known follow-up, not something to solve here.
- [Compile-classpath HTTP check misses a library that pulls an HTTP client only at runtime] → The INTERNET-permission check and code review cover that gap. A runtime-classpath check would be wrong, because `:app` legitimately has OkHttp at runtime.
- [Empty skeleton modules (capture, model-setup, data modules) have no code yet] → Acceptable: they set the boundaries now and cost little build time. Each later change fills one in.
- [minSdk 26 drops Android 7.x] → Accepted product decision (see proposal); no users yet.

## Migration Plan

Single-step migration on `main` (no users, nothing released). Rollback is reverting the change's commits. The theme move is mechanical: package name change only.

## Open Questions

- Exact library versions (Hilt, KSP, kotlinx.serialization plugin, Kover, navigation-suite through the BOM): chosen at implementation time from the newest stable releases compatible with AGP 9.3 / Kotlin 2.2. This doesn't change the approach or tasks.
