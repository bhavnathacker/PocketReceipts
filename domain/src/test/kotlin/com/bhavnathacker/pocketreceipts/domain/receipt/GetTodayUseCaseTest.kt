package com.bhavnathacker.pocketreceipts.domain.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetTodayUseCaseTest {

    @Test
    fun `test that invoke returns the date in the clock's time zone`() {
        // 23:30 UTC on 2 March is already 3 March in Kolkata (UTC+5:30).
        val clock = Clock.fixed(Instant.parse("2026-03-02T23:30:00Z"), ZoneId.of("Asia/Kolkata"))
        val underTest = GetTodayUseCase(clock = clock)

        assertThat(underTest()).isEqualTo(LocalDate.of(2026, 3, 3))
    }
}
