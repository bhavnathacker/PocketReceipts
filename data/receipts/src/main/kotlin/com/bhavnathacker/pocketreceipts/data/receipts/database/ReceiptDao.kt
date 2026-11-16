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
}
