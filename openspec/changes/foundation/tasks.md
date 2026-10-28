# Tasks

## 1. Version catalog and build-logic scaffolding

- [x] 1.1 Add catalog entries (Android library plugin, Hilt plugin + libraries, KSP plugin, kotlinx.serialization plugin + core, `androidx.hilt` lifecycle-viewmodel-compose, `material3-adaptive-navigation-suite` via BOM, `javax.inject`, Kover plugin), choosing the newest stable versions compatible with AGP 9.3 built-in Kotlin. Verify with `./gradlew help` that the catalog resolves.
- [x] 1.2 Create the `build-logic` included build (`build-logic/settings.gradle.kts`, `build-logic/convention/convention.gradle.kts`) that reads the root version catalog, and include it from `pluginManagement` in `settings.gradle.kts`. Verify `./gradlew help` succeeds with the configuration cache stored and then reused on a second run.
- [x] 1.3 Add the `includeModule(path)` helper to `settings.gradle.kts` that sets `buildFileName` to `<last segment>.gradle.kts`, and switch `:core:test` to it. Verify `./gradlew projects` lists `:core:test` and `./gradlew :core:test:test` still passes.

## 2. Convention plugins and module skeleton

- [x] 2.1 Implement `pocketreceipts.jvm.library` (Java 11, JUnit Platform, Kotlin `moduleName` from project path) and move `:core:test` onto it. Verify `./gradlew :core:test:build` passes.
- [x] 2.2 Implement `pocketreceipts.android.library`, `pocketreceipts.android.compose` and `pocketreceipts.android.application` with shared SDK constants (minSdk **26**, compile/target 37, Java 11, JUnit Platform, path-based Kotlin `moduleName`), and move `:app` onto the application plugin. Verify `./gradlew :app:assembleDebug` passes and the merged manifest shows `minSdkVersion 26`.
- [x] 2.3 Implement `pocketreceipts.hilt` (Hilt plugin + KSP + hilt-android/compiler) and `pocketreceipts.android.feature` (library + compose + hilt + `:domain` + `:core:ui` + viewmodel/navigation3 dependencies + kotlinx.serialization plugin). Verify a throwaway feature build file using only the plugin id compiles (removed again after checking).
- [x] 2.4 Create `:domain` (JVM, `javax.inject` + coroutines only), `:core:ui`, `:core:ui-test`, `:feature:receipts`, `:feature:capture`, `:feature:ask`, `:feature:modelsetup`, `:feature:settings`, `:data:receipts`, `:data:ai`, `:data:model-download`, each with its `<module>.gradle.kts`, namespace `com.bhavnathacker.pocketreceipts.<layer>.<name>` and `src/main/kotlin`. Verify `./gradlew projects` lists all modules and `./gradlew assembleDebug` passes.
- [x] 2.5 Make `:app` depend on all feature and data modules, and verify there's no duplicate `META-INF/*.kotlin_module` packaging error for `:feature:receipts` and `:data:receipts`: `./gradlew :app:assembleDebug` passes, and the APK contains distinct `feature-receipts` / `data-receipts` module files.

## 3. Module dependency rules

- [x] 3.1 Write JUnit 5 tests in build-logic for the dependency-rule function (feature→data fails, feature→feature fails, data→feature fails, feature→domain/core:ui passes, data→domain passes), then implement the function until they pass (`./gradlew -p build-logic test`).
- [x] 3.2 Register `verifyModuleDependencies` from the library and feature conventions and wire it into `check`. Verify `./gradlew check` passes, then temporarily add `:data:receipts` to `:feature:receipts`, confirm `./gradlew :feature:receipts:verifyModuleDependencies` fails with a clear message, and revert.

## 4. Privacy checks (spec: privacy-guarantees)

- [x] 4.1 Write JUnit 5 tests for the privacy rule functions (HTTP-client denylist match with allowed project `:data:model-download`; tracking-SDK denylist match; INTERNET permission source evaluation, including the allowed source), then implement them, with denylists as constants in one file. Verify `./gradlew -p build-logic test` passes.
- [x] 4.2 Write Gradle TestKit functional tests with JVM fixture projects for `verifyNoHttpClient`: a direct dependency in a non-allowed module fails and names module + dependency; the allowed module passes; an `api` leak into a consumer fails and names the consumer. Implement the task (compile classpath, configuration-cache-safe providers) until the tests pass.
- [x] 4.3 Implement `verifyNoTrackingSdks` on `:app` (debug + release runtime classpaths) and wire all privacy tasks into `check`. Verify `./gradlew check` passes, then temporarily add `firebase-analytics` to `:app`, confirm the check fails naming module + SDK, and revert.
- [x] 4.4 Implement `verifyInternetPermissionSources` on `:app` (own manifest + dependency manifests via the `android-manifest` artifact view). Verify: `./gradlew check` passes and the merged debug manifest has no INTERNET permission; temporarily declaring INTERNET in `:app` fails the check naming `:app`; declaring it in `:data:model-download` passes; then revert both.
- [x] 4.5 Add a "Privacy guarantees" section to `README.md` describing the three build checks and how to run them (`./gradlew check`). Verify the documented command runs as written.

## 5. Hilt and dispatchers

- [x] 5.1 Add `@IoDispatcher` and `@DefaultDispatcher` qualifiers in `:domain` (`...domain.dispatcher`) with KDoc. Verify `./gradlew :domain:build` passes and `:domain` has no Android or Hilt dependency (`./gradlew :domain:dependencies --configuration compileClasspath`).
- [x] 5.2 Annotate `PocketReceiptsApplication` with `@HiltAndroidApp` (keeping the Timber setup) and `MainActivity` with `@AndroidEntryPoint`, and add `DispatcherModule` in `:app` providing `Dispatchers.IO` / `Dispatchers.Default`. Verify `./gradlew :app:assembleDebug` passes (Hilt graph compiles) and the app launches on a device without crashing.

## 6. Shared UI module

- [x] 6.1 Move `Color.kt`, `Theme.kt` and `Type.kt` from `:app` into `:core:ui` (`com.bhavnathacker.pocketreceipts.core.ui.theme`) unchanged apart from the package, and update imports. Verify `./gradlew :app:assembleDebug` passes and the app still renders with dynamic color on Android 12+.
- [x] 6.2 Add Material Symbols vector drawables for Receipts, Ask and Settings (outlined and filled) to `:core:ui`. Verify each shows correctly in a Compose `@Preview`.

## 7. Navigation shell (spec: app-navigation)

- [x] 7.1 Write JUnit 5 tests for `TopLevelNavigator` (select Receipts → `[Receipts]`; select Ask from Receipts → `[Receipts, Ask]`; select Settings from Ask → `[Receipts, Settings]`; reselect Ask → unchanged; selected tab = top-level key nearest the top of the stack), then implement it in `:app` until `./gradlew :app:testDebugUnitTest` passes.
- [x] 7.2 In `:feature:receipts`, `:feature:ask` and `:feature:settings`, add the `@Serializable` `NavKey`, the `EntryProviderScope<NavKey>` entries extension and a placeholder screen headed with the destination name (string resources, content insets applied), each with a `@Preview`. Verify `./gradlew assembleDebug` passes.
- [x] 7.3 Write instrumented Compose UI tests in `:app` (JUnit 4, through `:core:ui-test`): app opens on Receipts with it selected; the three destinations appear in order with labels; selecting Ask shows the Ask heading; reselecting Ask and then pressing back shows Receipts; back from Settings shows Receipts; rotation keeps Settings (`StateRestorationTester`); compact width shows the bar and medium width shows the rail (`DeviceConfigurationOverride.ForcedSize`).
- [x] 7.4 Implement the shell in `MainActivity`: `TopLevelDestination` enum, `rememberNavBackStack(ReceiptsKey)`, `NavigationSuiteScaffold` with explicit `layoutType` from `currentWindowAdaptiveInfo()`, and `NavDisplay` using the feature entries. Remove the `Greeting` sample. Verify the 7.3 tests pass with `./gradlew :app:connectedDebugAndroidTest`.
- [x] 7.5 Manually check on a device or emulator: edge-to-edge with gesture and three-button navigation (no item covered), predictive back preview from Ask/Settings, back from Receipts closes the app, and Ask is restored after "Don't keep activities" process recreation. Record the result in the change notes.

## 8. Continuous integration

- [x] 8.1 Apply Kover in the root project with every module as a `kover` dependency, giving one aggregated report. Verify `./gradlew koverXmlReport` produces a report that includes `:app` and `:core:test` classes (if Kover doesn't support AGP 9 yet, record that and skip, per the design risk).
- [ ] 8.2 Add `.github/workflows/ci.yml` (push to `main` + pull requests: JDK 25, `gradle/actions/setup-gradle`, `./gradlew check koverXmlReport`, upload lint and Kover reports). Verify the workflow runs green on GitHub after pushing.
- [ ] 8.3 Add the CI status badge and the module overview (module graph + dependency rules) to `README.md`. Verify the badge renders and shows the latest CI result.

## 9. Project documentation

- [x] 9.1 Update `.claude/CLAUDE.md`: Dependency Flow (feature and data modules both depend on domain; features never depend on data or other features; `:app` is the composition root), the file-structure example (`feature/capture/` with `presentation/` only, plus `data/receipts/`), and the minSdk (26) in Build Setup. Verify the updated text matches design D1–D3 and the build setup that now exists.

## 10. Integration check

- [x] 10.1 From a clean checkout, run `./gradlew clean check assembleDebug` twice and confirm the second run reuses the configuration cache. Run `./gradlew :app:connectedDebugAndroidTest` on a device. Everything passes.

## Workflow follow-up

- Commit and push directly to `main` (project rule: no feature branches).
- Archive the change with `/opsx:archive` after review; this creates `openspec/specs/app-navigation` and `openspec/specs/privacy-guarantees`.
