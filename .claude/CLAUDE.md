# Project Rules

## Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew test                          # JVM unit tests, all modules
./gradlew check                         # unit tests, lint, module-dependency and privacy checks (what CI runs)
./gradlew connectedAndroidTest          # instrumented + Compose UI tests, all modules, needs a device
./gradlew -p build-logic test           # convention plugin and privacy rule tests
./gradlew lint                          # Android lint

# Single test class / method
./gradlew :app:testDebugUnitTest --tests "com.bhavnathacker.pocketreceipts.<package>.<ClassName>Test"
./gradlew :app:testDebugUnitTest --tests "com.bhavnathacker.pocketreceipts.<package>.<ClassName>Test.test that <method> <action>"
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bhavnathacker.pocketreceipts.<package>.<ClassName>Test
```

## Build Setup

- **AGP 9** (`9.3.3`) with its built-in Kotlin support: there is no `org.jetbrains.kotlin.android` plugin, only `org.jetbrains.kotlin.plugin.compose`. Don't add the Kotlin Android plugin; use AGP 9 DSL and APIs.
- Module build setup lives in convention plugins in the `build-logic` included build (`pocketreceipts.android.application`, `.android.library`, `.android.compose`, `.android.feature`, `.android.room`, `.jvm.library`, `.hilt`, `.privacy`). New modules apply a convention plugin instead of configuring AGP/Kotlin directly, and are added in `settings.gradle.kts` with `includeModule(":group:name")`, which sets the `<name>.gradle.kts` build file name.
- Privacy is enforced by the build (`./gradlew check`): only `:data:model-download` may depend on an HTTP client or add the INTERNET permission, no analytics/crash-reporting SDKs are allowed, and no usage-logging upload backend may be registered (`verifyNoUsageLoggingBackend`). See README "Privacy guarantees".
- **ML Kit usage logging is switched off in `app/src/main/AndroidManifest.xml`**: ML Kit (`:feature:capture` document scanner, `:data:ocr` text recognition) needs the classes of `com.google.android.datatransport:transport-backend-cct`, so it can't be excluded (ML Kit crashes). Instead the app manifest removes its `backend:…CctBackendFactory` meta-data; `verifyInternetPermissionSources` accepts the library's INTERNET declaration only while that removal is there. Never remove it. The app manifest must not remove INTERNET: `:data:model-download` needs it to download the on-device model. `:data:ocr`'s androidTest manifest removes both for its test APK.
- **On-device AI**: receipts and Ask's spending questions are read by Gemma 4 E2B with LiteRT-LM (`:data:ai`). The model (2.6 GB, pinned URL, size and SHA-256 in `ModelSpec`) is downloaded by `:data:model-download` with `DownloadManager` into `getExternalFilesDir("models")`, which the backup rules exclude. The app is gated on it (`ModelSetupGate` in `:feature:modelsetup`); `:app` instrumented tests replace `ModelRepositoryModule` with `FakeModelRepository`, so they never download it. The engine is shared (`LiteRtModelRuntime`): `CaptureViewModel` keeps it loaded while the scanner is open and `AskViewModel` while Ask is on screen (between `onShown()`/`onHidden()`, driven by `LifecycleStartEffect`), both with `KeepModelReadyUseCase`, and it's closed 2 minutes after its last use, or at once when the app goes to the background with no holder. While the ML Kit scanner (a Play services activity) is in front, the app counts as background, so never release the engine on background alone. The engine runs on the GPU only where `System.loadLibrary("OpenCL")` succeeds (`OpenClGpuSupport`; `:data:ai`'s manifest declares `libOpenCL.so`/`libvndksupport.so` as optional native libraries), and on the CPU otherwise: without OpenCL, LiteRT-LM's GPU backend falls back to WebGPU, which ran for about 15 minutes on the emulator before failing, so never try the GPU without that check. A GPU load failure, or a reading failure on a GPU engine, switches the runtime to the CPU for the rest of the process (the failed read is run again on the CPU). There is no rule-based fallback. Ask's model only reads a question into a category and period (`SpendingQueryChecker` validates it); totals are summed exactly in SQL (`ReceiptDao.currencyTotals`), never by the model. The receipt prompt (`ReceiptPromptBuilder`) and its reply limit are separate from Ask's and stay unchanged.
- **Kotlin 2.4**: LiteRT-LM is compiled with Kotlin 2.4 metadata, so the project's Kotlin must be 2.3 or later.
- All dependency and plugin versions live in the version catalog `gradle/libs.versions.toml`; reference them via `libs.*` aliases. Compose library versions come from the Compose BOM.
- compileSdk/targetSdk 37, minSdk 26, Java 11 source/target. The Gradle daemon runs on JDK 25 (toolchain auto-provisioned via foojay, see `gradle/gradle-daemon-jvm.properties`).
- Gradle configuration cache is enabled (`gradle.properties`), so build logic must be configuration-cache compatible.
- JVM unit tests (`app/src/test`) run on JUnit 5 via `useJUnitPlatform()` (Jupiter from `junit-bom`). Instrumented tests (`app/src/androidTest`) stay on JUnit 4, which `AndroidJUnitRunner` requires.
- `:app` instrumented tests run with `HiltTestRunner` and are `@HiltAndroidTest` classes hosted in the debug-only `HiltTestActivity`. Feature modules test screens statelessly with `createComposeRule()` (the activity comes from `:core:ui-test`).
- R8 keep rules go in `app/src/main/keepRules/` (AGP 9 picks up every file there), not `proguard-rules.pro`. Release optimization is currently disabled in `app/build.gradle.kts`.

## Project Structure & Architecture

### Clean Architecture
- **Three layers**: presentation (`:feature:*`), domain (`:domain`, pure Kotlin) and data (`:data:*`).
- **Dependency flow**: feature and data modules depend on `:domain`; `:domain` depends on neither. Features never depend on data modules or on other features. `:app` is the composition root and binds data implementations to domain interfaces with Hilt. `verifyModuleDependencies` (part of `./gradlew check`) enforces this. `:core:*` modules may depend on `:domain`.

### Modules
| Module | Contents |
|---|---|
| `:app` | `@HiltAndroidApp`, `MainActivity`, `ModelSetupGate` wiring, the Navigation 3 shell (`PocketReceiptsApp`, `TopLevelDestination`: Receipts and Ask) |
| `:domain` | Models, repository interfaces, use cases, checkers (`ReceiptValidator`, `ReceiptFieldsChecker`, `SpendingQueryChecker`) |
| `:feature:receipts` | Receipt list, detail and add/edit/review form |
| `:feature:capture` | ML Kit document scanner flow; reads the scan with `ScanReceiptUseCase` |
| `:feature:ask` | Spending questions: question field, examples, answer |
| `:feature:modelsetup` | Model download/verify/load setup screen (`ModelSetupGate`) |
| `:data:receipts` | Room database, DAO, mappers, `DefaultReceiptRepository` |
| `:data:ocr` | ML Kit text recognition, scan image deletion |
| `:data:ai` | Gemma via LiteRT-LM: shared `LiteRtModelRuntime`, receipt and question prompts and reply parsers |
| `:data:model-download` | `DownloadManager` download and SHA-256 check of the model; the only module with network access |
| `:core:ui` | Theme, `AppIcons`, `MoneyFormatter`, `ReceiptCategoryLabelMapper` and category strings |
| `:core:test`, `:core:ui-test` | `CoroutineMainDispatcherExtension`; Compose test host |

### File Structure
```
feature/ask/
├── src/main/kotlin/com/bhavnathacker/pocketreceipts/feature/ask/presentation/
│   ├── AskViewModel.kt, AskScreen.kt, AskNavigation.kt
│   ├── model/          # UI state and UI models
│   └── mapper/         # domain → UI mappers
└── ask.gradle.kts
data/receipts/
├── src/main/kotlin/com/bhavnathacker/pocketreceipts/data/receipts/
│   ├── database/       # Room database, entities, DAO
│   ├── mapper/         # entity ↔ domain mappers
│   ├── repository/     # DefaultReceiptRepository
│   └── di/             # Hilt bindings
├── schemas/            # exported Room schemas (committed)
└── receipts.gradle.kts
```

## Naming Conventions
- **ViewModels**: `{Feature}ViewModel` (e.g. `AskViewModel`, `ReceiptFormViewModel`)
- **Use cases**: `{Action}UseCase` (e.g. `AnswerSpendingQuestionUseCase`, `SaveReceiptUseCase`)
- **Repositories**: interfaces `{Thing}Repository` in `:domain`; implementations `Default{Thing}Repository`, or named after their technology (`LiteRtSpendingQuestionRepository`, `MlKitTextRecognitionRepository`)
- **Mappers**: `{Source}{Target}Mapper` or `{Thing}UiMapper` (e.g. `ReceiptEntityMapper`, `SpendingAnswerUiMapper`)
- **Tests**: `{ClassName}Test.kt`
- **Packages**: `com.bhavnathacker.pocketreceipts.feature.{feature}.presentation`, `...domain.{area}`, `...data.{module}`
- **Build files**: named after the module (`feature/ask/ask.gradle.kts`, `data/model-download/model-download.gradle.kts`); only `:app` uses `app/build.gradle.kts`

## Code Organization Patterns

### Dependency Injection with Hilt
- **ViewModels**: see [viewmodel-conventions.md](skills/viewmodel/viewmodel-conventions.md) (`@HiltViewModel`, private `MutableStateFlow` exposed as `StateFlow`, use cases and mappers only, never repositories). Navigation arguments use assisted injection (`ReceiptFormViewModel.Factory`).
- **Activities**: `@AndroidEntryPoint`.
- **Modules**: `@Module @InstallIn(SingletonComponent::class)`, with `@Binds` for interface bindings (e.g. `AiDataModule`). Data implementations are `internal`.
- **Dispatchers**: inject `@IoDispatcher` / `@DefaultDispatcher` (`:domain`), and `@InferenceDispatcher` (the single LiteRT-LM thread) in `:data:ai`; never hard-code `Dispatchers.IO`.

### Repository Pattern
- Interfaces live in `:domain` (e.g. `ReceiptRepository`, `SpendingQuestionRepository`, `ModelRepository`); implementations live in data modules and wrap one SDK or store each (Room, ML Kit, LiteRT-LM, `DownloadManager`).
- One-shot calls are `suspend`; changing data is a `Flow` (`monitorReceipts()`).

### Use Case Pattern
- One business operation per use case, `@Inject constructor`, `operator fun invoke`, depending on repository interfaces and other domain classes:
  ```kotlin
  class AnswerSpendingQuestionUseCase @Inject constructor(
      private val spendingQuestionRepository: SpendingQuestionRepository,
      private val spendingQueryChecker: SpendingQueryChecker,
      private val receiptRepository: ReceiptRepository,
      private val getToday: GetTodayUseCase,
  ) {
      suspend operator fun invoke(question: String): SpendingAnswer = ...
  }
  ```
- `suspend` for one-shot operations, `Flow` for streams. See [usecase-conventions.md](skills/usecase/usecase-conventions.md).

### Model output is never trusted
- Gemma's replies are parsed leniently in `:data:ai` and then validated in `:domain` (`ReceiptFieldsChecker`, `SpendingQueryChecker`). Invalid values are dropped, never guessed. Gemma never computes amounts: Ask's totals are summed in SQL.

### State Management
- See [viewmodel-conventions.md](skills/viewmodel/viewmodel-conventions.md): a single `uiState: StateFlow`, updated with `_uiState.update { }`; one-shot events with `StateEvent`/`StateEventWithContent` from `compose-state-events`.

## Testing Patterns

### Unit Testing
- **JUnit 5** (`@TestInstance(PER_CLASS)` for stateless classes, `@ExtendWith(CoroutineMainDispatcherExtension::class)` for ViewModels), **Mockito-Kotlin**, **Turbine** for flows, **Google Truth** for assertions, `runTest` for coroutines.
  ```kotlin
  @ExtendWith(CoroutineMainDispatcherExtension::class)
  class AskViewModelTest {
      @Test
      fun `test that onAsk shows the mapped answer`() = runTest { ... }
  }
  ```
- Classes that use Android's ICU (`MoneyFormatter`, `DateRangeFormatter`) are tested on a device (`androidTest`), because the JVM doesn't have it.

### Test Method Naming
- `test that <method> <action>`() — e.g. `test that onShown keeps the model ready`()
- `test that <method> <action> when <cause>`() — e.g. `test that invoke returns Unanswerable when the question is unsupported`()
- Instrumented tests (JUnit 4) use the same words with underscores: `test_that_ask_is_disabled_while_the_question_is_blank`.

### Test Structure
- **Use case tests**: [usecase-test-conventions.md](skills/usecase/usecase-test-conventions.md)
- **ViewModel tests**: [viewmodel-test-conventions.md](skills/viewmodel/viewmodel-test-conventions.md)
- **Mapper tests**: [mapper-test-conventions.md](skills/mapper/mapper-test-conventions.md)
- **Repository tests**: mocked DAOs or fake engines (`EngineFactory { _, _ -> ... }`); DAO queries are tested on an in-memory Room database (`ReceiptDaoTest`).
- **Screens**: stateless composables tested with `createComposeRule()` in each feature module; the app shell with `@HiltAndroidTest` in `:app`, where `FakeModelRepository` replaces the model download (its `modelPath()` is `null`, so the model is never loaded).

## Data Layer Patterns

### Mappers
- See [mapper-conventions.md](skills/mapper/mapper-conventions.md): `internal class` with `@Inject constructor` and `operator fun invoke`, one responsibility each (e.g. `ReceiptEntityMapper`, `ReceiptCategoryMapper`, which maps categories to stable database keys).

### Database
- **Room**: Use Room for local database operations. Apply the `pocketreceipts.android.room` convention plugin (Room Gradle plugin + KSP); it exports schemas to `<module>/schemas` (e.g. `data/receipts/schemas`). Commit schema JSON files and bump the version with a migration for every schema change.
- **No encryption**: The receipts database is a plain Room database (no SQLCipher), provided as a Hilt singleton. DAO methods are `suspend` or return `Flow`, so Room opens it off the main thread.
- **Backups**: The `database` domain is excluded from cloud backup and device transfer. Keep it that way: receipt data never leaves the phone.
- **Exact money**: amounts are `Long` minor units with an ISO 4217 code (`Money`); never use floating point for money.

## UI Patterns

### Jetpack Compose
- **State**: screens are split into a `…Route` (collects the ViewModel's `uiState` with `collectAsStateWithLifecycle`) and a stateless `…Screen` that previews and tests use.
- **Navigation**: Navigation 3 (`androidx.navigation3`): destinations are `NavKey`s on an app-owned back stack rendered by `NavDisplay`; each feature registers its entries (`askEntries()`, `receiptsEntries()`).
- **Theming**: Material 3 (`PocketReceiptsTheme`), edge-to-edge, light and dark previews (`@PreviewLightDark`).
- **Strings**: all user-visible text in string resources; plurals with `pluralStringResource`.

## Security & Privacy
- No API keys or secrets are needed or committed.
- Privacy rules are enforced by the build (see Build Setup and README "Privacy guarantees"): no analytics or crash-reporting SDKs, no HTTP clients outside `:data:model-download`, no usage-log upload backend. Never log receipt contents or Ask questions.

## Development Workflow

### Spec-driven changes (OpenSpec)
- Behaviour is specified in `openspec/specs/<capability>/spec.md`. Each change is proposed in `openspec/changes/<name>/` (proposal, delta specs, design, tasks), implemented with `/opsx:apply`, and archived with `/opsx:archive` after its PR is merged, which syncs the delta specs into the main specs.

### Git Workflow
- **Feature branches**: Do each change on a branch off `main` (e.g. `feature/<openspec-change-name>`). Commit there and push the branch for review.
- **Review before merge**: Open a pull request into `main`. Merge only after the owner has reviewed it and explicitly approved the merge. Never commit or push directly to `main`.
- **CI**: `.github/workflows/ci.yml` runs the build-logic tests, `./gradlew check` and Kover coverage on every push to `main` and every pull request.

## Code Formatting & Indentation  
  
### Indentation Rules  
- **Use 4 spaces for indentation** (never tabs)  
- Apply consistent 4-space indentation for all nested blocks  
- Function parameters spanning multiple lines should use 4-space indentation  
- Compose modifier chains should be indented with 4 spaces per level  
- Constructor parameters should follow 4-space indentation when multi-line  
  
### Specific Patterns  
- **Kotlin/Java**: Standard 4-space indentation for all code blocks  
- **Compose UI**: Modifier chains and nested components use 4-space indentation  
- **Function Calls**: Multi-line function calls align parameters with 4-space indentation  
- **Class Constructors**: Multi-line constructor parameters use 4-space indentation  
  
### Formatting Tools  
- Configure your IDE to use 4 spaces instead of tabs  
- Set continuation indent to 4 spaces  
- Enable "Use tab character" = false in IDE settings

## Android Knowledge Base

When you need to look up Android documentation, best practices, or API guidance, use the Android CLI Knowledge Base:
- **Search**: `android docs search '<query>'`
- **Fetch**: `android docs fetch <kb-url>` (using the `kb://` URLs returned by search)

If the `android` CLI is not installed (i.e., `which android` returns nothing), prompt the user to install it from https://developer.android.com/tools/agents before proceeding.
