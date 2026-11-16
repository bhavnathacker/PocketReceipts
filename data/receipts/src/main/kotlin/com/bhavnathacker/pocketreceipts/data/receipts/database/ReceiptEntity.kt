package com.bhavnathacker.pocketreceipts.data.receipts.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A row of the `receipts` table. Columns hold primitives only: dates are
 * epoch days, amounts are minor units, categories are stable string keys.
 */
@Entity(
    tableName = "receipts",
    indices = [Index(value = ["purchase_date", "created_at"])],
)
internal data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    @ColumnInfo(name = "purchase_date") val purchaseDate: Long,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val category: String,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
