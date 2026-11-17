package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DateCandidateParserTest {
    private lateinit var underTest: DateCandidateParser

    private val enIn = Locale.forLanguageTag("en-IN")
    private val enUs = Locale.forLanguageTag("en-US")
    private val deDe = Locale.forLanguageTag("de-DE")

    @BeforeAll
    fun setUp() {
        val clock = Clock.fixed(LocalDate.of(2026, 11, 16).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        underTest = DateCandidateParser(getToday = GetTodayUseCase(clock))
    }

    @Test
    fun `test that invoke reads an ambiguous date day first when locale is en-IN`() {
        assertThat(underTest(listOf("Date: 03/02/2026"), enIn)).containsExactly(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke reads an ambiguous date month first when locale is en-US`() {
        assertThat(underTest(listOf("Date: 03/02/2026"), enUs)).containsExactly(LocalDate.of(2026, 3, 2))
    }

    @Test
    fun `test that invoke reads day first when only that order is a valid date`() {
        assertThat(underTest(listOf("25/12/2025"), enUs)).containsExactly(LocalDate.of(2025, 12, 25))
    }

    @Test
    fun `test that invoke reads month first when only that order is a valid date`() {
        assertThat(underTest(listOf("12/25/2025"), enIn)).containsExactly(LocalDate.of(2025, 12, 25))
    }

    @Test
    fun `test that invoke reads a dot separated date`() {
        assertThat(underTest(listOf("03.02.2026 14:05"), deDe)).containsExactly(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke reads an ISO date regardless of locale`() {
        assertThat(underTest(listOf("2026-02-03"), enUs)).containsExactly(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke maps a two digit year to this century`() {
        assertThat(underTest(listOf("03-02-26"), enIn)).containsExactly(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke reads day month name year`() {
        assertThat(underTest(listOf("3 Feb 2026"), enUs)).containsExactly(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke reads month name day comma year`() {
        assertThat(underTest(listOf("Feb 3, 2026"), enIn)).containsExactly(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke reads an upper case short month with a two digit year`() {
        assertThat(underTest(listOf("03-FEB-26"), enIn)).containsExactly(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke reads a month name in the device language`() {
        assertThat(underTest(listOf("3. März 2026"), deDe)).containsExactly(LocalDate.of(2026, 3, 3))
    }

    @Test
    fun `test that invoke drops dates after today`() {
        assertThat(underTest(listOf("Valid until 31/12/2026", "Date 01/11/2026"), enIn))
            .containsExactly(LocalDate.of(2026, 11, 1))
    }

    @Test
    fun `test that invoke keeps the order of appearance`() {
        assertThat(underTest(listOf("01/10/2026", "15/09/2026"), enIn))
            .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 15))
            .inOrder()
    }

    @Test
    fun `test that invoke removes duplicate dates`() {
        assertThat(underTest(listOf("01/10/2026", "2026-10-01", "1 Oct 2026"), enIn))
            .containsExactly(LocalDate.of(2026, 10, 1))
    }

    @Test
    fun `test that invoke returns at most five dates`() {
        val lines = (1..7).map { "0$it/01/2026" }

        assertThat(underTest(lines, enIn)).hasSize(5)
    }

    @Test
    fun `test that invoke ignores impossible dates`() {
        assertThat(underTest(listOf("32/13/2026", "31/02/2026"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke ignores amounts, times and phone numbers`() {
        assertThat(underTest(listOf("TOTAL 1,234.50", "12:30", "Tel 020 7946 0000"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke ignores words that are not month names`() {
        assertThat(underTest(listOf("2 Milk 2026"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke reads a date glued to its label`() {
        assertThat(underTest(listOf("Date:-12/11/2026", "Dt.13/11/26", "Date-14/11/2026"), enIn))
            .containsExactly(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14))
            .inOrder()
    }

    @Test
    fun `test that invoke reads a date with spaces around the separators`() {
        assertThat(underTest(listOf("Date : 12 / 11 / 2026"), enIn)).containsExactly(LocalDate.of(2026, 11, 12))
    }

    @Test
    fun `test that invoke reads a date followed by a time after a comma or slash`() {
        assertThat(underTest(listOf("12/11/2026,10:30", "13-11-2026/10:30"), enIn))
            .containsExactly(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13))
            .inOrder()
    }

    @Test
    fun `test that invoke reads a two digit year after an apostrophe`() {
        assertThat(underTest(listOf("Date: 12 Nov' 26"), enIn)).containsExactly(LocalDate.of(2026, 11, 12))
    }

    @Test
    fun `test that invoke reads letters OCR mistakes for digits inside a date`() {
        assertThat(underTest(listOf("Date:12/1l/2O26"), enIn)).containsExactly(LocalDate.of(2026, 11, 12))
    }

    @Test
    fun `test that invoke ignores a date that continues into another number`() {
        assertThat(underTest(listOf("Ref 12/11/2026/5/1", "Lot 1.12/11/2026"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke reads a month name with a zero OCR reads for the letter O`() {
        assertThat(underTest(listOf("SELF Bill No C68642 31-0ct-26 21:44:2", "2 N0v 2025"), enIn))
            .containsExactly(LocalDate.of(2026, 10, 31), LocalDate.of(2025, 11, 2))
            .inOrder()
    }

    @Test
    fun `test that invoke keeps a zero that ends a day before a month name`() {
        assertThat(underTest(listOf("20Nov2025"), enIn)).containsExactly(LocalDate.of(2025, 11, 20))
    }
}
