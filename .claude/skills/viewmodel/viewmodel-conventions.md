# ViewModel Production Code Conventions

## Class Structure

```kotlin
@HiltViewModel
internal class MyFeatureViewModel @Inject constructor(
    private val monitorItemsUseCase: MonitorItemsUseCase,
    private val performActionUseCase: PerformActionUseCase,
    private val itemUiMapper: ItemUiMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyFeatureUiState())
    val uiState: StateFlow<MyFeatureUiState> = _uiState.asStateFlow()

    init {
        monitorItems()
    }

    private fun monitorItems() {
        viewModelScope.launch {
            monitorItemsUseCase()
                .catch { Timber.e(it, "Failed to load items") }
                .collect { items ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = items.map { itemUiMapper(it) },
                        )
                    }
                }
        }
    }

    fun onSomeAction(param: String) {
        viewModelScope.launch {
            runCatching {
                performActionUseCase(param)
            }.onFailure {
                Timber.e(it, "Failed to perform action")
            }
        }
    }
}
```

## Key Rules

- **`@HiltViewModel` + `@Inject constructor`** — always.
- **`internal` visibility** — ViewModels and mappers in feature modules should be `internal class`.
- **Dependencies**: inject use cases and mappers only — **never** repositories or data layer code.
- **State**: back the state with a **private `MutableStateFlow`** and expose it read-only:
  ```kotlin
  private val _uiState = MutableStateFlow(MyFeatureUiState())
  val uiState: StateFlow<MyFeatureUiState> = _uiState.asStateFlow()
  ```
  - Expose a single `val uiState: StateFlow<UiState>` — no other public properties, and never expose the `MutableStateFlow` itself.
  - Update state with **`_uiState.update { it.copy(...) }`**, which is atomic. Do not use `_uiState.value = _uiState.value.copy(...)`.
  - For domain flows: start collecting in `init` (through a private function such as `monitorItems()`) or from an explicit UI call, and fold each emission into the state with `update`.
  - For one-shot events (navigation, snackbar, etc.): see One-Shot Events below.
- **Error handling**: `runCatching { }.onFailure { Timber.e(it, "msg") }` for suspend actions; `.catch { }` in flow chains.
- **Coroutines**: `viewModelScope.launch { }`.
- **Logging**: `Timber` only — never `android.util.Log`.

## One-Shot Events

For one-shot UI events (navigation, snackbar, dialog triggers), use `StateEventWithContent<T>` (or `StateEvent` when there is no payload) from `de.palm.composestateevents`, held as a field in the UI state. Trigger with `triggered(value)` and reset with `consumed()` — both through `_uiState.update`. Add one `on<Event>Consumed()` function per event, which the UI calls after handling it (e.g. from `EventEffect`).

```kotlin
@HiltViewModel
internal class MyFeatureViewModel @Inject constructor(
    private val performActionUseCase: PerformActionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyFeatureUiState())
    val uiState: StateFlow<MyFeatureUiState> = _uiState.asStateFlow()

    fun onNavigate(id: Long) {
        viewModelScope.launch {
            runCatching {
                performActionUseCase(id)
            }.onSuccess { receiptId ->
                _uiState.update { it.copy(navigationEvent = triggered(receiptId)) }
            }.onFailure {
                Timber.e(it, "Navigation failed")
            }
        }
    }

    fun onNavigationEventConsumed() {
        _uiState.update { it.copy(navigationEvent = consumed()) }
    }
}
```

## Assisted Injection (Runtime Parameters)

When runtime parameters from navigation are needed:

- **Never pass the Navigation 3 `NavKey` itself** as a ViewModel parameter — pass only the values the ViewModel needs.
- **Single parameter**: pass directly with `@Assisted`.
- **Multiple parameters**: create an `Args` data class inside the ViewModel.

```kotlin
@HiltViewModel(assistedFactory = MyFeatureViewModel.Factory::class)
internal class MyFeatureViewModel @AssistedInject constructor(
    private val someUseCase: SomeUseCase,
    @Assisted val args: Args,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: Args): MyFeatureViewModel
    }

    data class Args(val id: String, val type: SomeType)

    private val _uiState = MutableStateFlow(MyFeatureUiState())
    val uiState: StateFlow<MyFeatureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            someUseCase(args.id)
                .catch { e -> Timber.e(e, "Failed to load") }
                .collect { data -> _uiState.update { it.copy(isLoading = false, data = data) } }
        }
    }
}
```

## UI State

Use a `@Stable` flat `data class` with default values — it represents a single evolving state that `_uiState.update { it.copy(...) }` modifies:

```kotlin
@Stable
data class MyFeatureUiState(
    val isLoading: Boolean = true,
    val items: List<ItemUiModel> = emptyList(),
    val navigationEvent: StateEventWithContent<Long> = consumed(),
)
```

A `@Stable` sealed interface (e.g. `Loading` / `Data` / `Error`) is acceptable when a screen has genuinely distinct states that share no fields. Updates then replace the whole value (`_uiState.update { MyFeatureUiState.Data(...) }`) instead of using `copy`.

## Mappers

```kotlin
internal class MyEntityMapper @Inject constructor() {
    operator fun invoke(domain: DomainEntity): UiModel =
        UiModel(
            id = domain.id,
            displayName = domain.name,
        )
}
```

- `internal class` — mappers in feature modules should be internal.
- `@Inject constructor()` — always.
- `operator fun invoke(...)` — for callable syntax. Only add `suspend` if the mapper performs actual async work.
- Single responsibility: domain entity -> UI model.
- Place in `mapper/` subpackage.

## File Placement

```
<module>/src/main/kotlin/<package>/
    <Name>ViewModel.kt
    model/
        <Name>UiState.kt
        <UiModelClasses>.kt
    mapper/
        <Name>Mapper.kt
<module>/src/test/kotlin/<package>/
    <Name>ViewModelTest.kt
    mapper/
        <Name>MapperTest.kt
```
