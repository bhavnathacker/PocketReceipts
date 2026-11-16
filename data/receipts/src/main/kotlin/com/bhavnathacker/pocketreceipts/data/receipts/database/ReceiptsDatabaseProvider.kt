package com.bhavnathacker.pocketreceipts.data.receipts.database

import android.content.Context
import androidx.room.Room
import com.bhavnathacker.pocketreceipts.data.receipts.security.DatabasePassphraseProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Opens the SQLCipher-encrypted [ReceiptsDatabase] once, off the main thread,
 * and hands out the same instance afterwards.
 *
 * Loading the native library, unwrapping the passphrase and opening the file
 * all happen on [ioDispatcher] on the first [get], never during injection. If
 * opening fails, [get] throws and the next call tries again; the database file
 * is never deleted.
 */
internal class ReceiptsDatabaseProvider(
    private val context: Context,
    private val passphraseProvider: DatabasePassphraseProvider,
    private val ioDispatcher: CoroutineDispatcher,
    private val databaseName: String = ReceiptsDatabase.NAME,
) {
    private val mutex = Mutex()

    @Volatile
    private var database: ReceiptsDatabase? = null

    suspend fun get(): ReceiptsDatabase =
        database ?: mutex.withLock {
            database ?: withContext(ioDispatcher) { open() }.also { database = it }
        }

    private fun open(): ReceiptsDatabase {
        System.loadLibrary(SQLCIPHER_LIBRARY)
        val factory = SupportOpenHelperFactory(passphraseProvider.getPassphrase())
        val database = Room.databaseBuilder(context, ReceiptsDatabase::class.java, databaseName)
            .openHelperFactory(factory)
            .build()
        try {
            // Open now, so a wrong key or a corrupt file fails here rather than in a later query.
            database.openHelper.writableDatabase
        } catch (e: Exception) {
            database.close()
            throw e
        }
        return database
    }

    companion object {
        private const val SQLCIPHER_LIBRARY = "sqlcipher"
    }
}
