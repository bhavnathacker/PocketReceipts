# Mapper Test Conventions

## Test Class Structure

### Simple Mapper (No Dependencies, Synchronous)

```kotlin
class ReceiptUiModelMapperTest {
    private lateinit var underTest: ReceiptUiModelMapper

    @BeforeEach
    fun setUp() {
        underTest = ReceiptUiModelMapper()
    }

    @Test
    fun `test that display name uses title when available`() {
        val receipt = createReceipt(title = "Weekly groceries", merchant = "Merchant")

        val result = underTest(receipt)

        assertThat(result.displayName).isEqualTo("Weekly groceries")
    }

    @Test
    fun `test that display name uses file name when both title and merchant are null`() {
        val receipt = createReceipt(title = null, merchant = null)

        val result = underTest(receipt)

        assertThat(result.displayName).isEqualTo("receipt_001.jpg")
    }

    private fun createReceipt(
        id: Long = 1L,
        fileName: String = "receipt_001.jpg",
        title: String? = "Title",
        merchant: String? = "Merchant",
        category: ReceiptCategory = ReceiptCategory.OTHER,
    ) = Receipt(
        id = id,
        fileName = fileName,
        title = title,
        merchant = merchant,
        category = category,
    )
}
```

### Composed Mapper (Mocked Dependencies)

```kotlin
class ReceiptDetailMapperTest {
    private lateinit var underTest: ReceiptDetailMapper

    private val merchantUiMapper = mock<MerchantUiMapper>()
    private val lineItemUiMapper = mock<LineItemUiMapper>()
    private val receiptTotalsUiMapper = mock<ReceiptTotalsUiMapper>()

    @BeforeEach
    fun setUp() {
        underTest = ReceiptDetailMapper(
            merchantUiMapper = merchantUiMapper,
            lineItemUiMapper = lineItemUiMapper,
            receiptTotalsUiMapper = receiptTotalsUiMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            merchantUiMapper,
            lineItemUiMapper,
            receiptTotalsUiMapper,
        )
    }

    @Test
    fun `test that merchant is mapped from merchant ui mapper`() {
        val expectedMerchant = mock<MerchantUiModel>()
        whenever(merchantUiMapper(any())).thenReturn(expectedMerchant)

        val result = underTest(mock<Receipt>(), currencyCode = "EUR")

        assertThat(result.merchant).isEqualTo(expectedMerchant)
    }

    @Test
    fun `test that line item mapper is called with correct arguments`() {
        val receipt = mock<Receipt>()

        underTest(receipt, currencyCode = "EUR")

        verify(lineItemUiMapper).invoke(receipt, "EUR")
    }
}
```

### Suspend Mapper

```kotlin
@ExtendWith(CoroutineMainDispatcherExtension::class)
class ReceiptThumbnailMapperTest {
    private lateinit var underTest: ReceiptThumbnailMapper

    @BeforeEach
    fun setUp() {
        underTest = ReceiptThumbnailMapper(
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that receipts are mapped to thumbnails`() = runTest {
        val receipts = listOf(mock<Receipt>(), mock<Receipt>())

        val result = underTest(receipts)

        assertThat(result).hasSize(2)
    }
}
```

## Key Rules

- **JUnit 5** — `@Test`, `@BeforeEach`, `@AfterEach`.
- **`@ExtendWith(CoroutineMainDispatcherExtension::class)`** — only for suspend mappers. Simple synchronous mappers do **not** need it. The extension lives in the `:core:test` module (add `testImplementation(project(":core:test"))`).
- **`underTest`** — always name the mapper under test `underTest`.
- **Dependencies** — mock with `mock<T>()`, create mapper in `@BeforeEach`, reset mocks in `@AfterEach`.
- **No `@AfterEach`** needed for simple mappers with no mocked dependencies.
- **Test names** — backtick style: `` `test that X when Y` ``.
- **Assertions** — Google Truth: `assertThat(...).isEqualTo(...)`.
- **`= runTest { ... }`** — only for suspend mapper tests. Synchronous mapper tests do **not** need it.
- **No Turbine** — mappers do not emit Flows, so Turbine is not needed (unlike ViewModel tests).

## Testing Patterns

### Happy Path

Test the standard mapping with typical input values:

```kotlin
@Test
fun `test that id is mapped correctly`() {
    val input = DomainEntity(id = "123", name = "Test")

    val result = underTest(input)

    assertThat(result.id).isEqualTo("123")
}
```

### Null Input

If the mapper accepts nullable input, test with `null`:

```kotlin
@Test
fun `test that null input returns null`() {
    val result = underTest(null)

    assertThat(result).isNull()
}
```

### Null Fields

Test when individual fields in the input are null:

```kotlin
@Test
fun `test that display name falls back to merchant when title is null`() {
    val input = createReceipt(title = null)

    val result = underTest(input)

    assertThat(result.displayName).isEqualTo(input.merchant)
}
```

### List Mapping

Test empty list, single item, and multiple items:

```kotlin
@Test
fun `test that empty list returns empty list`() {
    val result = underTest(emptyList())

    assertThat(result).isEmpty()
}

@Test
fun `test that list items are mapped individually`() {
    val items = listOf(
        DomainItem(id = "1"),
        DomainItem(id = "2"),
    )

    val result = underTest(items)

    assertThat(result).hasSize(2)
    assertThat(result[0].id).isEqualTo("1")
    assertThat(result[1].id).isEqualTo("2")
}
```

### Edge Cases

Test boundary values, empty strings, zero values, and enum exhaustiveness:

```kotlin
@Test
fun `test that empty string name maps to empty display name`() {
    val input = createEntity(name = "")

    val result = underTest(input)

    assertThat(result.displayName).isEmpty()
}
```

### Verifying Dependent Mapper Calls

When testing composed mappers, verify that dependent mappers are called with the correct arguments:

```kotlin
@Test
fun `test that totals mapper is called with correct receipt`() {
    val receipt = mock<Receipt>()

    underTest(receipt, currencyCode = "EUR")

    verify(receiptTotalsUiMapper).invoke(receipt)
}
```

### @StringRes Mapper

Test that each input maps to the correct string resource:

```kotlin
@Test
fun `test that FOOD maps to category_food string`() {
    val result = underTest(ReceiptCategory.FOOD)

    assertThat(result).isEqualTo(R.string.category_food)
}

@Test
fun `test that null maps to category_other string`() {
    val result = underTest(null)

    assertThat(result).isEqualTo(R.string.category_other)
}
```

## Test Data Helpers

Use `create*()` factory functions to build test input objects with sensible defaults:

```kotlin
private fun createReceipt(
    id: Long = 1L,
    fileName: String = "receipt_001.jpg",
    title: String? = "Title",
    merchant: String? = "Merchant",
    category: ReceiptCategory = ReceiptCategory.OTHER,
) = Receipt(
    id = id,
    fileName = fileName,
    title = title,
    merchant = merchant,
    category = category,
)
```

- Place helper functions at the bottom of the test class as `private` functions.
- Use default parameter values so each test only specifies the fields it cares about.
- Prefer constructing real domain objects over mocking them, unless the domain object has many required fields.

## Required Imports

```kotlin
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
// For suspend mappers only:
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.extension.ExtendWith
```