package com.bhavnathacker.pocketreceipts.domain.receipt

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Today's date in the device's time zone: the default purchase date and the
 * latest date a receipt may have.
 */
class GetTodayUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(): LocalDate = LocalDate.now(clock)
}
