package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import java.time.LocalDate

/**
 * Candidate values found on a scanned receipt, for the user to confirm.
 *
 * Every list is ordered best first, so its first element is the preselection.
 * Lists may be empty when nothing suitable was found.
 *
 * @property merchant The most likely merchant name, or null if none was found.
 * @property dates Purchase date candidates, none of them after today.
 * @property totals Total amount candidates: the labelled total first, then the
 * other amounts, largest first.
 */
data class ReceiptScan(
    val merchant: String?,
    val dates: List<LocalDate>,
    val totals: List<Money>,
)
