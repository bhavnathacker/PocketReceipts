package com.bhavnathacker.pocketreceipts.data.receipts.di

import android.content.Context
import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptsDatabaseProvider
import com.bhavnathacker.pocketreceipts.data.receipts.repository.DefaultReceiptRepository
import com.bhavnathacker.pocketreceipts.data.receipts.security.DatabasePassphraseProvider
import com.bhavnathacker.pocketreceipts.data.receipts.security.KeystoreDatabasePassphraseProvider
import com.bhavnathacker.pocketreceipts.domain.dispatcher.IoDispatcher
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReceiptsDataModule {

    @Binds
    abstract fun bindReceiptRepository(repository: DefaultReceiptRepository): ReceiptRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabasePassphraseProvider(
            @ApplicationContext context: Context,
        ): DatabasePassphraseProvider =
            KeystoreDatabasePassphraseProvider(
                keyFile = { File(context.noBackupFilesDir, KeystoreDatabasePassphraseProvider.KEY_FILE_NAME) },
                keyAlias = KeystoreDatabasePassphraseProvider.KEY_ALIAS,
            )

        @Provides
        @Singleton
        fun provideReceiptsDatabaseProvider(
            @ApplicationContext context: Context,
            passphraseProvider: DatabasePassphraseProvider,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): ReceiptsDatabaseProvider =
            ReceiptsDatabaseProvider(
                context = context,
                passphraseProvider = passphraseProvider,
                ioDispatcher = ioDispatcher,
            )
    }
}
