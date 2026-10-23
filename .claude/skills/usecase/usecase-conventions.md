# UseCase Production Code Conventions

## Class Structure

```kotlin
class GetPricingUseCase @Inject constructor(
    private val pricingRepository: PricingRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean): Pricing =
        pricingRepository.getPricing(forceRefresh)
}
```

## Key Rules

- **`@Inject constructor`** — always. No manual instantiation.
- **`operator fun invoke(...)`** — single public method, callable syntax (`useCase(params)` instead of `useCase.invoke(params)`).
- **Single responsibility** — one use case = one business operation.
- **Dependencies**: inject repository interfaces and other use cases only — **never** data layer implementations, gateways, or facades directly.
- **No scope annotations** — do not annotate use cases with `@Singleton`, `@ViewModelScoped`, or any other scope. Use cases are stateless and lightweight; repository singletons handle shared state.
- **No Android framework dependencies** — use cases must not import or depend on `Context`, `Activity`, or any Android SDK class.

## Return Type Patterns

### Suspend one-shot
For single operations (fetch, save, delete, compute):
```kotlin
class GetPricingUseCase @Inject constructor(
    private val pricingRepository: PricingRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean): Pricing =
        pricingRepository.getPricing(forceRefresh)
}
```

### Flow monitoring
For observing/streaming data. **Not suspend** — Flow is cold and starts on collection:
```kotlin
class MonitorReceiptUpdatesUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    operator fun invoke(receiptId: ReceiptId): Flow<Receipt> =
        receiptRepository.monitorReceipts().mapNotNull { receipts ->
            receipts.firstOrNull { it.id == receiptId }
        }
}
```

### Synchronous
For pure computations, formatting, validation — neither suspend nor Flow:
```kotlin
class ValidateEmailUseCase @Inject constructor() {
    operator fun invoke(email: String): Boolean =
        email.matches(Regex("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
}
```

### Nullable returns
When the absence of data is a valid state:
```kotlin
class GetReceiptByIdUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    suspend operator fun invoke(receiptId: ReceiptId): Receipt? =
        receiptRepository.getReceipt(receiptId)
}
```

## Complexity Levels

### Simple delegation
Single repository call, expression body:
```kotlin
class GetLatestReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    suspend operator fun invoke(): Receipt? =
        receiptRepository.getLatestReceipt()
}
```

### Business logic
Conditions, transformations, multiple repository calls:
```kotlin
class IsDuplicateReceiptUseCase @Inject constructor(
    private val getReceiptsUseCase: GetReceiptsUseCase,
) {
    suspend operator fun invoke(receipt: Receipt): Boolean {
        return getReceiptsUseCase().any { existing ->
            existing.merchant == receipt.merchant &&
                existing.total == receipt.total &&
                existing.date == receipt.date
        }
    }
}
```

### Orchestration
Composing multiple use cases:
```kotlin
class SaveReceiptUseCase @Inject constructor(
    private val isDuplicateReceiptUseCase: IsDuplicateReceiptUseCase,
    private val addReceiptUseCase: AddReceiptUseCase,
    private val updateReceiptUseCase: UpdateReceiptUseCase,
) {
    suspend operator fun invoke(receipt: Receipt) =
        if (isDuplicateReceiptUseCase(receipt)) {
            updateReceiptUseCase(receipt)
        } else {
            addReceiptUseCase(receipt)
        }
}
```

### Flow composition
Combining flows with operators:
```kotlin
class MonitorReceiptImagesUseCase @Inject constructor(
    private val receiptImageRepository: ReceiptImageRepository,
    private val receiptRepository: ReceiptRepository,
) {
    operator fun invoke(receiptIds: List<ReceiptId>): Flow<List<ReceiptImage>> = flow {
        emit(populateImages(receiptIds))
        emitAll(monitorImages(receiptIds))
    }

    private suspend fun populateImages(receiptIds: List<ReceiptId>): List<ReceiptImage> =
        receiptIds.mapNotNull { receiptImageRepository.getReceiptImage(it) }

    private fun monitorImages(receiptIds: List<ReceiptId>): Flow<List<ReceiptImage>> =
        receiptRepository.monitorReceiptUpdates()
            .filter { updatedIds -> updatedIds.any { it in receiptIds } }
            .map { populateImages(receiptIds) }
}
```

### Coroutine orchestration
Parallel operations with `async`/`supervisorScope`:
```kotlin
class ExportReceiptsUseCase @Inject constructor(
    private val exportReceiptUseCase: ExportReceiptUseCase,
) {
    suspend operator fun invoke(receipts: List<Receipt>): Map<ReceiptId, String> =
        supervisorScope {
            receipts.map { receipt ->
                async {
                    runCatching { exportReceiptUseCase(receipt.id) }
                        .getOrNull()
                        ?.let { receipt.id to it }
                }
            }.awaitAll().filterNotNull().toMap()
        }
}
```

## Naming Conventions

- **Class name**: `{Action}UseCase` — descriptive verb-based naming.
- **Action verbs**: `Get` (fetch/read), `Set`/`Save` (write), `Monitor` (Flow-returning observer), `Delete`/`Remove`, `Update`, `Validate`, `Is`/`Are`/`Has` (boolean checks), `Create`, `Submit`, `Broadcast`, `Export`, `Copy`, `Login`/`Logout`.
- **Private helper methods** are allowed for complex implementations.
- **Internal data classes** are allowed for state management within complex use cases.

## Visibility

- Use cases are `class` (public, no modifier) — they are part of the domain API consumed by all modules.

## File Placement

All use cases live in the `domain` module under feature subpackages:

```
domain/src/main/kotlin/com/bhavnathacker/pocketreceipts/domain/usecase/{package}/
    {Name}UseCase.kt
```

**Never place use cases in the root `usecase/` directory.** All use cases go into a feature subpackage (e.g., `usecase/receipt/`, `usecase/merchant/`, `usecase/export/`).

When choosing a subpackage:
1. Use the subpackage that matches the use case's primary domain area.
2. Check existing subpackages first.
3. If no existing subpackage fits, create a new one with a concise, lowercase name describing the domain area.

## Error Handling

- Use cases can throw exceptions — let callers handle them. ViewModels use `runCatching` + `onFailure { Timber.e(...) }`.
- For use cases that need internal error handling: `runCatching` with meaningful error propagation.
- Do **not** catch and swallow exceptions silently.

## Dispatcher Usage

- Most use cases do **not** specify a dispatcher — they inherit the caller's context.
- If heavy computation is needed, inject a dispatcher via qualifier and use `flowOn()` or `withContext()`:
  ```kotlin
  class ProcessDataUseCase @Inject constructor(
      private val repository: DataRepository,
      @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
  ) {
      operator fun invoke(): Flow<ProcessedData> =
          repository.monitorData()
              .map { heavyTransformation(it) }
              .flowOn(defaultDispatcher)
  }
  ```
- **Never** use `Dispatchers.IO` or `Dispatchers.Default` directly — always inject via qualifier annotations.

## Legacy Patterns (Avoid in New Code)

### `fun interface` SAM pattern

```kotlin
// ❌ Legacy — do not use for new code
fun interface GetPricing {
    suspend operator fun invoke(forceRefresh: Boolean): Pricing
}

// Requires separate implementation + Hilt binding:
class DefaultGetPricing @Inject constructor(
    private val repository: PricingRepository,
) : GetPricing {
    override suspend fun invoke(forceRefresh: Boolean) =
        repository.getPricing(forceRefresh)
}

// ✅ Preferred — single concrete class
class GetPricingUseCase @Inject constructor(
    private val repository: PricingRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean): Pricing =
        repository.getPricing(forceRefresh)
}
```

### `interface` + `Default*` implementation

```kotlin
// ❌ Legacy — do not use for new code
interface ReloadReceiptDatabase {
    suspend operator fun invoke(isForceReload: Boolean)
}

class DefaultReloadReceiptDatabase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) : ReloadReceiptDatabase {
    override suspend fun invoke(isForceReload: Boolean) { ... }
}

// ✅ Preferred — single concrete class
class ReloadReceiptDatabaseUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    suspend operator fun invoke(isForceReload: Boolean) { ... }
}
```

### Abstract base classes

Only justified when genuinely sharing complex logic across multiple use cases (rare). Prefer composition over inheritance — inject shared logic as a separate use case.

## Anti-Patterns

- **Multiple public methods** — violates single responsibility. Split into separate use cases.
- **Mutable state** — use cases should be stateless. Move state to the repository layer.
- **Android framework dependencies** — use cases must not import `Context`, `Activity`, etc.
- **Scope annotations** (`@Singleton`, `@ViewModelScoped`) — use cases are lightweight; don't scope them.
- **Catching and swallowing exceptions** — let them propagate to the caller.