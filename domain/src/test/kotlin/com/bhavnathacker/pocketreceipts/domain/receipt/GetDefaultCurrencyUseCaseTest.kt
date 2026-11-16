package com.bhavnathacker.pocketreceipts.domain.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDefaultCurrencyUseCaseTest {
    private lateinit var underTest: GetDefaultCurrencyUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetDefaultCurrencyUseCase()
    }

    @Test
    fun `test that invoke returns GBP when locale is en-GB`() {
        assertThat(underTest(Locale.forLanguageTag("en-GB"))).isEqualTo("GBP")
    }

    @Test
    fun `test that invoke returns EUR when locale is de-DE`() {
        assertThat(underTest(Locale.forLanguageTag("de-DE"))).isEqualTo("EUR")
    }

    @Test
    fun `test that invoke returns INR when locale is en-IN`() {
        assertThat(underTest(Locale.forLanguageTag("en-IN"))).isEqualTo("INR")
    }

    @Test
    fun `test that invoke returns EUR when locale has no region`() {
        assertThat(underTest(Locale.forLanguageTag("en"))).isEqualTo("EUR")
    }

    @Test
    fun `test that invoke returns EUR when region has no currency`() {
        assertThat(underTest(Locale.forLanguageTag("en-AQ"))).isEqualTo("EUR")
    }
}
