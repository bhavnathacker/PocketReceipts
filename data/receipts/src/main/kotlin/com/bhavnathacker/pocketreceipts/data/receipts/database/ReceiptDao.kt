package com.bhavnathacker.pocketreceipts.data.receipts.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ReceiptDao {

    /**
     * All receipts, newest purchase date first; same date: most recently added first.
     */
    @Query("SELECT * FROM receipts ORDER BY purchase_date DESC, created_at DESC, id DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<ReceiptEntity?>

    @Query("SELECT created_at FROM receipts WHERE id = :id")
    suspend fun getCreatedAt(id: Long): Long?

    /** Inserts a new row and returns its id. */
    @Insert
    suspend fun insert(entity: ReceiptEntity): Long

    @Update
    suspend fun update(entity: ReceiptEntity)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * The totals of the receipts dated [fromEpochDay] to [toEpochDay] (both included) and, unless
     * [category] is `null`, with that category key: one row per currency, the currency with the
     * most receipts first, then by currency code. SQLite adds integers exactly.
     */
    @Query(
        """
        SELECT currency_code, SUM(total_minor) AS total_minor, COUNT(*) AS receipt_count
        FROM receipts
        WHERE purchase_date BETWEEN :fromEpochDay AND :toEpochDay
            AND (:category IS NULL OR category = :category)
        GROUP BY currency_code
        ORDER BY receipt_count DESC, currency_code
        """,
    )
    suspend fun currencyTotals(fromEpochDay: Long, toEpochDay: Long, category: String?): List<CurrencyTotalRow>
}
