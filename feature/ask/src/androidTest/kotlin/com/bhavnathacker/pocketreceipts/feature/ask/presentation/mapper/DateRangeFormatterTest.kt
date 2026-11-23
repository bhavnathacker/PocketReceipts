package com.bhavnathacker.pocketreceipts.feature.ask.presentation.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

/**
 * Runs on a device: [DateRangeFormatter] uses Android's ICU, which the JVM doesn't have.
 * Spaces are normalised, since ICU uses thin and narrow no-break spaces around dashes.
 */
@RunWith(AndroidJUnit4::class)
class DateRangeFormatterTest {
    private lateinit var underTest: DateRangeFormatter

    @Before
    fun setUp() {
        underTest = DateRangeFormatter()
    }

    private fun format(from: LocalDate, to: LocalDate, locale: Locale) =
        underTest(from, to, locale).replace(Regex("[\\u2009\\u202F\\u00A0]"), " ")

    @Test
    fun test_that_a_month_shares_its_month_and_year_in_en_GB() {
        assertThat(format(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), Locale.UK)).isEqualTo("1 – 31 Oct 2026")
    }

    @Test
    fun test_that_a_month_follows_the_en_US_order() {
        val text = format(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), Locale.US)

        assertThat(text).startsWith("Oct 1")
        assertThat(text).endsWith("31, 2026")
    }

    @Test
    fun test_that_a_quarter_shares_only_its_year() {
        val text = format(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), Locale.UK)

        assertThat(text).isEqualTo("1 Oct – 31 Dec 2026")
    }

    @Test
    fun test_that_a_range_across_years_shows_both_years() {
        val text = format(LocalDate.of(2025, 12, 15), LocalDate.of(2026, 1, 14), Locale.UK)

        assertThat(text).isEqualTo("15 Dec 2025 – 14 Jan 2026")
    }

    @Test
    fun test_that_a_single_day_shows_one_date() {
        assertThat(format(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 21), Locale.UK)).isEqualTo("21 Nov 2026")
    }

    @Test
    fun test_that_dates_do_not_shift_with_the_device_time_zone() {
        val zone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Kiritimati"))

            assertThat(format(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), Locale.UK)).isEqualTo("1 – 31 Oct 2026")
        } finally {
            java.util.TimeZone.setDefault(zone)
        }
    }
}
