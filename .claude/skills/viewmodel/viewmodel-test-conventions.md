# ViewModel Test Conventions

## Test Class Structure

```kotlin
@ExtendWith(CoroutineMainDispatcherExtension::class)
class MyFeatureViewModelTest {
    private lateinit var underTest: MyFeatureViewModel

    private val monitorItemsUseCase = mock<MonitorItemsUseCase>()
    private val performActionUseCase = mock<PerformActionUseCase>()
    private val itemUiMapper = mock<ItemUiMapper>()

    @BeforeEach
    fun setUp() {
        monitorItemsUseCase.stub {
            on { invoke() } doReturn emptyFlow()
        }
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorItemsUseCase,
            performActionUseCase,
            itemUiMapper,
        )
    }

    private fun initViewModel() {
        underTest = MyFeatureViewModel(
            monitorItemsUseCase = monitorItemsUseCase,
            performActionUseCase = performActionUseCase,
            itemUiMapper = itemUiMapper,
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        monitorItemsUseCase.stub {
            on { invoke() } doReturn flow { awaitCancellation() }
        }
        initViewModel()

        assertThat(underTest.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `test that state contains mapped items when use case emits`() = runTest {
        val item = mock<Item>()
        val uiItem = mock<ItemUiModel>()
        monitorItemsUseCase.stub {
            on { invoke() } doReturn flow {
                emit(listOf(item))
                awaitCancellation()
            }
        }
        itemUiMapper.stub {
            on { invoke(item) } doReturn uiItem
        }
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).containsExactly(uiItem)
        }
    }

    @Test
    fun `test that navigation event is triggered when action succeeds`() = runTest {
        performActionUseCase.stub {
            onBlocking { invoke(1L) } doReturn 1L
        }
        initViewModel()

        underTest.onNavigate(1L)

        val event = underTest.uiState.value.navigationEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        assertThat((event as StateEventWithContentTriggered).content).isEqualTo(1L)
    }
}
```

## Key Rules

- **JUnit 5** with `@ExtendWith(CoroutineMainDispatcherExtension::class)` — `viewModelScope` runs on `Dispatchers.Main`, which doesn't exist in JVM unit tests (see CoroutineMainDispatcherExtension below).
- ViewModel variable always named **`underTest`**.
- All dependencies mocked with **`mock<T>()`**.
- **Stub before constructing.** The ViewModel starts collecting in `init`, so stubs must be in place before it is created. Put default stubs in **`@BeforeEach`**, override them in the test if needed, then call a private **`initViewModel()`** helper. Mocks reset in **`@AfterEach`**.
- Test names: **backtick style** — `` `test that X when Y` ``.
- All tests use **`= runTest { ... }`**.
- Flow testing with **Turbine**: `underTest.uiState.test { awaitItem() }`.
- Assertions with **Google Truth**: `assertThat(...).isEqualTo(...)`.
- One-shot events: assert `StateEventWithContentTriggered` / `StateEventWithContentConsumed` on the state field, and test that each `on<Event>Consumed()` resets it to consumed.
- Verification: `verify(mock).invoke(args)`, `argumentCaptor<T>()`.
- Flow stubs: `stub { on { invoke() } doReturn flow { emit(...); awaitCancellation() } }`.
- **Property mocking**: When stubbing properties on mock objects, use `this.propertyName` to avoid ambiguity:
   ```kotlin
   // Correct:
   mock<Receipt> {
       on { this.merchant } doReturn merchant
   }

   // Incorrect — Mockito may resolve to the wrong property:
   mock<Receipt> {
       on { merchant } doReturn merchant
   }
   ```

## CoroutineMainDispatcherExtension

A JUnit 5 extension that sets `Dispatchers.Main` to a test dispatcher before the test class runs and resets it afterwards. It lives in the `:core:test` module:

```
core/test/src/main/kotlin/com/bhavnathacker/pocketreceipts/core/test/extension/CoroutineMainDispatcherExtension.kt
```

- Add `testImplementation(project(":core:test"))` to any module whose tests use it.
- Default (`UnconfinedTestDispatcher`): annotate the class with `@ExtendWith(CoroutineMainDispatcherExtension::class)`.
- Different dispatcher (e.g. `StandardTestDispatcher`): register it programmatically instead:
  ```kotlin
  companion object {
      @JvmField
      @RegisterExtension
      val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
  }
  ```

## Required Imports

```kotlin
import app.cash.turbine.test
import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
```
