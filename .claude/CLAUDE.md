# Project Rules

## Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest          # instrumented + Compose UI tests (app/src/androidTest), needs a device
./gradlew lint                          # Android lint

# Single test class / method
./gradlew :app:testDebugUnitTest --tests "com.bhavnathacker.pocketreceipts.<package>.<ClassName>Test"
./gradlew :app:testDebugUnitTest --tests "com.bhavnathacker.pocketreceipts.<package>.<ClassName>Test.test that <method> <action>"
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bhavnathacker.pocketreceipts.<package>.<ClassName>Test
```

## Build Setup

- **AGP 9** (`9.3.3`) with its built-in Kotlin support: there is no `org.jetbrains.kotlin.android` plugin, only `org.jetbrains.kotlin.plugin.compose`. Don't add the Kotlin Android plugin; use AGP 9 DSL and APIs.
- All dependency and plugin versions live in the version catalog `gradle/libs.versions.toml`; reference them via `libs.*` aliases. Compose library versions come from the Compose BOM.
- compileSdk/targetSdk 37, minSdk 24, Java 11 source/target. The Gradle daemon runs on JDK 25 (toolchain auto-provisioned via foojay, see `gradle/gradle-daemon-jvm.properties`).
- Gradle configuration cache is enabled (`gradle.properties`), so build logic must be configuration-cache compatible.
- JVM unit tests (`app/src/test`) run on JUnit 5 via `useJUnitPlatform()` (Jupiter from `junit-bom`). Instrumented tests (`app/src/androidTest`) stay on JUnit 4, which `AndroidJUnitRunner` requires.
- R8 keep rules go in `app/src/main/keepRules/` (AGP 9 picks up every file there), not `proguard-rules.pro`. Release optimization is currently disabled in `app/build.gradle.kts`.

## Project Structure & Architecture

### Clean Architecture Principles
- **Three-Layer Architecture**: Strict separation between presentation, domain, and data layers
- **Dependency Flow**: Dependencies flow inward (data → domain → presentation)
- **Module Organization**: Features organized into separate modules (app, data, domain, core, feature, etc.)

### Module Structure
- **App Module**: Main application module with `@HiltAndroidApp` annotation
- **Domain Module**: Business logic, use cases, and repository interfaces
- **Data Module**: Repository implementations, data sources, and mappers
- **Feature Modules**: Self-contained feature modules
- **Core Modules**: Shared utilities, UI components, and common functionality

## File Organization & Naming Conventions

### File Structure
- **Module Structure**: Each module follows consistent internal structure
  ```
  feature/scan/
  ├── src/main/kotlin/com/bhavnathacker/pocketreceipts/feature/scan/
  │   ├── data/           # Repository implementations, mappers
  │   └── presentation/    # ViewModels, UI components
  └── scan.gradle.kts
  ```

### Naming Conventions
- **Classes**: Use PascalCase for class names
  - ViewModels: `{Feature}ViewModel` (e.g., `GlobalStateViewModel`)
  - Use Cases: `{Action}UseCase` (e.g., `LoginUseCase`)
  - Repositories: `Default{Feature}Repository` (e.g., `DefaultAccountRepository`)
  - Mappers: `{Source}Mapper` (e.g., `TransferMapper`)

- **Files**: Use descriptive file names
  - Test files: `{ClassName}Test.kt`
  - Module files: `{module-name}.gradle.kts`

- **Packages**: Follow reverse domain naming
  - `com.bhavnathacker.pocketreceipts.feature.{feature}.{layer}`
  - `com.bhavnathacker.pocketreceipts.domain.{domain}`
  - `com.bhavnathacker.pocketreceipts.data.{data-type}`

### Build File Naming
- **Module Build Files**: Use module name as build file name
  - `app/app.gradle.kts`
  - `feature/home/home.gradle.kts`
  - `core/ui-components/ui-components.gradle.kts`

## Code Organization Patterns

### Dependency Injection with Hilt  
- **ViewModels**: See [viewmodel-conventions.md](.claude/skills/viewmodel/viewmodel-conventions.md) for ViewModel conventions (Hilt, StateFlow, use case injection).
- **Activities**: Use `@AndroidEntryPoint`
- **Modules**: Create `@Module` classes with `@InstallIn(SingletonComponent::class)`
  ```kotlin
  @Module 
  @InstallIn(SingletonComponent::class)
  class DatabaseHandlerModule {
      @Singleton
      @Provides
      fun provideDbHandler(): DatabaseHandler = ...
  }
  ```
- **Entry Points**: Use `@EntryPoint` for non-Android classes that need injection

### Repository Pattern
- **Interfaces**: Define repository interfaces in domain layer
  ```kotlin
  interface AccountRepository {
      suspend fun getAccountInfo(): AccountInfo
      fun monitorAccountBlocked(): Flow<AccountBlocked>
  }
  ```
- **Implementations**: Implement repositories in data layer
  ```kotlin
  @Singleton
  class DefaultAccountRepository @Inject constructor(
      private val accountDataSource: AccountDataSource
  ) : AccountRepository {
      override suspend fun getAccountInfo(): AccountInfo = ...
  }
  ```
- **Gateway Pattern**: Use gateway interfaces to abstract SDK access
- **Facade Pattern**: Implement facades for complex SDK interactions

### Use Case Pattern
- **Single Responsibility**: Each use case handles one business operation
  ```kotlin
  class LoginUseCase @Inject constructor(
      private val loginRepository: LoginRepository,
      private val chatLogoutUseCase: ChatLogoutUseCase
  ) {
      suspend operator fun invoke(email: String, password: String): Flow<LoginStatus> = ...
  }
  ```
- **Constructor Injection**: Use `@Inject` constructor for dependencies
- **Repository Dependencies**: Use cases depend on repository interfaces, not implementations
- **Flow Returns**: Use cases return `Flow` or `StateFlow` for reactive programming
- **Use Cases**: See [usecase-conventions.md](.claude/skills/usecase/usecase-conventions.md) for UseCase conventions.

### State Management
- See [viewmodel-conventions.md](.claude/skills/viewmodel/viewmodel-conventions.md) for ViewModel state management conventions.

## Build Configuration

### Version Catalog
- **Centralized Dependencies**: Use the single version catalog `gradle/libs.versions.toml` for all library, plugin, and test dependency versions

### Build Types
- **Debug**: Development builds with debugging enabled
- **Release**: Production builds with minification and obfuscation

## Testing Patterns

### Unit Testing
- **JUnit 5**: Use JUnit 5 for all unit tests
  ```kotlin
  @ExtendWith(CoroutineMainDispatcherExtension::class)
  class BaseViewModelTest {
      @Test
      fun `test that state updates correctly`() = runTest {
          // Test implementation
      }
  }
  ```
- **Mockito**: Use Mockito for mocking dependencies
  ```kotlin
  private val mockUseCase = mock<MonitorAccountBlockedUseCase>()
  whenever(mockUseCase()).thenReturn(flowOf(AccountBlocked(false)))
  ```
- **Turbine**: Use Turbine for testing Flow emissions, for example UiState
  ```kotlin
  underTest.uiState.test {
      val state = awaitItem()
      assertThat(state.accountBlockedEvent).isNull()
  }
  ```
- **Truth**: Use Google Truth for assertions
- **Coroutines**: Use `runTest` for coroutine testing

### Test Method Naming
- **Format**: Use one of the following patterns for test method names:
  - `test that <method> <action>`() — e.g. `test that init does not call loginToFolderUseCase`()
  - `test that <method> <action> when <cause>`() — e.g. `test that init emits Loaded when login succeeds`()

### Test Structure
- **Repository Tests**: Test repository implementations with mocked dependencies
- **Use Case Tests**: See [usecase-test-conventions.md](.claude/skills/usecase/usecase-test-conventions.md) for UseCase test conventions.
- **ViewModel Tests**: See [viewmodel-test-conventions.md](.claude/skills/viewmodel/viewmodel-test-conventions.md) for ViewModel test conventions.
- **Mapper Tests**: See [mapper-test-conventions.md](.claude/skills/mapper/mapper-test-conventions.md) for Mapper test conventions.

### Test Modules
- **Core Test**: Shared test utilities and extensions
- **Core UI Test**: UI testing utilities and components
- **Test App Module**: Hilt test modules for dependency injection

## Data Layer Patterns

### Mappers
- See [mapper-conventions.md](.claude/skills/mapper/mapper-conventions.md) for Mapper conventions.
- **Entity Mappers**: Convert between domain entities and data entities
  ```kotlin
  internal class TransferMapper @Inject constructor(
      private val transferAppDataMapper: TransferAppDataMapper,
      private val transferTypeMapper: TransferTypeMapper
  ) {
      operator fun invoke(transfer: TransferEntity): Transfer = Transfer(
          uniqueId = transfer.uniqueId,
          transferType = transferTypeMapper(transfer.type, appData),
          // ... other mappings
      )
  }
  ```
- **Model Mappers**: Convert between different data representations
- **Null Safety**: Handle null values appropriately in mappers

### Database
- **Room**: Use Room for local database operations
- **SQLCipher**: Use SQLCipher for database encryption
- **Entities**: Define Room entities for data persistence
- **DAOs**: Create Data Access Objects for database operations

### Network
- **Error Handling**: Implement proper error handling for network calls

## UI Patterns

### Jetpack Compose
- **State Management**: Use `StateFlow` with Compose state
- **Navigation**: Use Navigation 3 (`androidx.navigation3`) for screen navigation — destinations are `NavKey`s on an app-owned back stack rendered by `NavDisplay`
- **Theming**: Use Material 3 theming system
- **Testing**: Use Compose testing utilities

## Security & Privacy

### Data Encryption
- **SQLCipher**: Encrypt local database
- **API Keys**: Never commit API keys to repository
- **Secrets**: Use environment variables for sensitive configuration

### Lint Rules
- **Custom Lint**: Use custom lint rules for code quality
- **Security Checks**: Implement security-focused lint checks
- **Code Style**: Enforce consistent code style across modules

## Performance & Optimization

### Build Performance
- **Parallel Tests**: Enable parallel test execution
- **Build Cache**: Use build cache for faster builds
- **Gradle Daemon**: Use Gradle daemon for faster builds
- **Module Optimization**: Optimize module dependencies

### Runtime Performance
- **Coroutines**: Use coroutines for asynchronous operations
- **Flow**: Use Flow for reactive programming
- **Memory Management**: Properly manage memory and resources
- **Background Processing**: Use WorkManager for background tasks

## Code Quality

### Linting
- **Android Lint**: Use Android Lint for code quality checks
- **Custom Rules**: Implement custom lint rules for project-specific requirements
- **CI Integration**: Integrate linting with CI/CD pipeline

### Documentation
- **KDoc**: Use KDoc for public API documentation
- **README**: Maintain comprehensive README files
- **Architecture Docs**: Document architectural decisions and patterns

## Development Workflow

### Git Workflow
- **Main Only**: Commit and push directly to `main`. Do not create feature branches.

### Local Development
- **Gradle Wrapper**: Use Gradle wrapper for consistent builds
- **Local Properties**: Use `local.properties` for local configuration
- **Debug Builds**: Use debug builds for development and testing

## Best Practices

### Kotlin
- **Null Safety**: Leverage Kotlin's null safety features
- **Extension Functions**: Use extension functions for utility functions
- **Data Classes**: Use data classes for simple data containers
- **Sealed Classes**: Use sealed classes for state management

### Android
- **Lifecycle Awareness**: Make components lifecycle-aware
- **Memory Leaks**: Avoid memory leaks with proper lifecycle management
- **Background Processing**: Use appropriate APIs for background work
- **Permissions**: Handle permissions properly and securely

### Architecture
- **SOLID Principles**: Follow SOLID principles for clean code
- **Dependency Inversion**: Depend on abstractions, not concretions
- **Single Responsibility**: Each class should have a single responsibility
- **Open/Closed**: Open for extension, closed for modification

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
