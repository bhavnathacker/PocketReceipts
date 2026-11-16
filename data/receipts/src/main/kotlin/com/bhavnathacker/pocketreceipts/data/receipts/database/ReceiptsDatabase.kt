package com.bhavnathacker.pocketreceipts.data.receipts.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The encrypted on-device receipts database. See `ReceiptsDatabaseProvider`
 * for how it is opened.
 */
@Database(
    entities = [ReceiptEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class ReceiptsDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao

    companion object {
        const val NAME = "receipts.db"
    }
}
