package com.bhavnathacker.pocketreceipts.domain.spending

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpendingQueryCheckerTest {
    private lateinit var underTest: SpendingQueryChecker

    private val today = LocalDate.of(2026, 11, 21)

    @BeforeAll
    fun setUp() {
        underTest = SpendingQueryChecker()
    }

    private fun check(category: String?, period: InterpretedPeriod) =
        underTest(InterpretedQuestion.Spending(category, period), today)

    @Test
    fun `test that invoke reads a month with its year`() {
        assertThat(check("GROCERIES", InterpretedPeriod.Month(2026, 10))).isEqualTo(
            SpendingQuery(ReceiptCategory.GROCERIES, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
        )
    }

    @Test
    fun `test that invoke uses this year for a month without a year when it has started`() {
        assertThat(check(null, InterpretedPeriod.Month(null, 11))).isEqualTo(
            SpendingQuery(null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)),
        )
    }

    @Test
    fun `test that invoke uses last year for a month without a year when it hasn't started`() {
        assertThat(check(null, InterpretedPeriod.Month(null, 12))).isEqualTo(
            SpendingQuery(null, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)),
        )
    }

    @Test
    fun `test that invoke keeps a future month when its year is stated`() {
        assertThat(check(null, InterpretedPeriod.Month(2026, 12))).isEqualTo(
            SpendingQuery(null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)),
        )
    }

    @Test
    fun `test that invoke ends February on its last day in a leap year`() {
        assertThat(check(null, InterpretedPeriod.Month(2024, 2))?.to).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun `test that invoke reads a quarter`() {
        assertThat(check("SHOPPING", InterpretedPeriod.Quarter(2026, 4))).isEqualTo(
            SpendingQuery(ReceiptCategory.SHOPPING, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)),
        )
    }

    @Test
    fun `test that invoke uses this year for a quarter without a year`() {
        assertThat(check(null, InterpretedPeriod.Quarter(null, 1))).isEqualTo(
            SpendingQuery(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
        )
    }

    @Test
    fun `test that invoke reads a year`() {
        assertThat(check(null, InterpretedPeriod.Year(2025))).isEqualTo(
            SpendingQuery(null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
        )
    }

    @Test
    fun `test that invoke uses this year for a year without a number`() {
        assertThat(check(null, InterpretedPeriod.Year(null))).isEqualTo(
            SpendingQuery(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
        )
    }

    @Test
    fun `test that invoke reads a range`() {
        assertThat(check("DINING", InterpretedPeriod.Range(" 2026-11-01 ", "2026-11-21"))).isEqualTo(
            SpendingQuery(ReceiptCategory.DINING, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 21)),
        )
    }

    @Test
    fun `test that invoke accepts a range of one day`() {
        assertThat(check(null, InterpretedPeriod.Range("2026-11-21", "2026-11-21"))).isEqualTo(
            SpendingQuery(null, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 21)),
        )
    }

    @Test
    fun `test that invoke reads all time with no date limits`() {
        assertThat(check(null, InterpretedPeriod.AllTime)).isEqualTo(SpendingQuery(null, null, null))
    }

    @Test
    fun `test that invoke reads a category in any case`() {
        assertThat(check(" groceries ", InterpretedPeriod.AllTime)?.category).isEqualTo(ReceiptCategory.GROCERIES)
    }

    @Test
    fun `test that invoke reads a blank category as every category`() {
        assertThat(check(" ", InterpretedPeriod.AllTime)).isEqualTo(SpendingQuery(null, null, null))
    }

    @Test
    fun `test that invoke rejects an unknown category`() {
        assertThat(check("PETS", InterpretedPeriod.Year(2025))).isNull()
    }

    @Test
    fun `test that invoke rejects month 0`() {
        assertThat(check(null, InterpretedPeriod.Month(2026, 0))).isNull()
    }

    @Test
    fun `test that invoke rejects month 13`() {
        assertThat(check(null, InterpretedPeriod.Month(2026, 13))).isNull()
    }

    @Test
    fun `test that invoke rejects a month without a number`() {
        assertThat(check(null, InterpretedPeriod.Month(2026, null))).isNull()
    }

    @Test
    fun `test that invoke rejects quarter 5`() {
        assertThat(check(null, InterpretedPeriod.Quarter(2026, 5))).isNull()
    }

    @Test
    fun `test that invoke rejects quarter 0`() {
        assertThat(check(null, InterpretedPeriod.Quarter(2026, 0))).isNull()
    }

    @Test
    fun `test that invoke rejects a year before 1900`() {
        assertThat(check(null, InterpretedPeriod.Year(1899))).isNull()
    }

    @Test
    fun `test that invoke rejects a year after 9999`() {
        assertThat(check(null, InterpretedPeriod.Year(10000))).isNull()
    }

    @Test
    fun `test that invoke rejects a range with an unparseable date`() {
        assertThat(check(null, InterpretedPeriod.Range("2026-11-01", "21/11/2026"))).isNull()
    }

    @Test
    fun `test that invoke rejects a range with a missing date`() {
        assertThat(check(null, InterpretedPeriod.Range(null, "2026-11-21"))).isNull()
    }

    @Test
    fun `test that invoke rejects a backwards range`() {
        assertThat(check(null, InterpretedPeriod.Range("2026-10-31", "2026-10-01"))).isNull()
    }

    @Test
    fun `test that invoke rejects an unsupported question`() {
        assertThat(underTest(InterpretedQuestion.Unsupported, today)).isNull()
    }
}
