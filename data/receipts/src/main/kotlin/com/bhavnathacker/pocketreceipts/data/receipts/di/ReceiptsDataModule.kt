package com.bhavnathacker.pocketreceipts.data.receipts.di

import android.content.Context
import androidx.room.Room
import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptDao
import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptsDatabase
import com.bhavnathacker.pocketreceipts.data.receipts.repository.DefaultReceiptRepository
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReceiptsDataModule {

    @Binds
    abstract fun bindReceiptRepository(repository: DefaultReceiptRepository): ReceiptRepository

    companion object {

        /** Room opens the file on the first query, which runs off the main thread. */
        @Provides
        @Singleton
        fun provideReceiptsDatabase(@ApplicationContext context: Context): ReceiptsDatabase =
            Room.databaseBuilder(context, ReceiptsDatabase::class.java, ReceiptsDatabase.NAME).build()

        @Provides
        fun provideReceiptDao(database: ReceiptsDatabase): ReceiptDao = database.receiptDao()
    }
}
