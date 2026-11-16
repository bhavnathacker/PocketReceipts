package com.bhavnathacker.pocketreceipts.data.receipts.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.data.receipts.security.DatabasePassphraseProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReceiptsDatabaseProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val passphrase = ByteArray(32) { it.toByte() }
    private val databaseName = "test_${UUID.randomUUID()}.db"
    private lateinit var databaseFile: File

    private val passphraseProvider = object : DatabasePassphraseProvider {
        override fun getPassphrase(): ByteArray = passphrase.copyOf()
    }

    @Before
    fun setUp() {
        databaseFile = context.getDatabasePath(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun test_that_get_returns_the_same_instance() = runTest {
        val underTest = ReceiptsDatabaseProvider(context, passphraseProvider, Dispatchers.IO, databaseName)

        val first = underTest.get()

        assertThat(underTest.get()).isSameInstanceAs(first)
        first.close()
    }

    @Test
    fun test_that_the_database_file_is_encrypted() = runTest {
        writeReceiptAndClose(merchant = "Corner Shop Plaintext")

        val header = databaseFile.readBytes().copyOf(16)
        assertThat(String(header, Charsets.US_ASCII)).isNotEqualTo("SQLite format 3\u0000")
        val allBytes = databaseFile.parentFile!!.listFiles { f -> f.name.startsWith(databaseName) }!!
            .flatMap { it.readBytes().asList() }
        assertThat(String(allBytes.toByteArray(), Charsets.ISO_8859_1)).doesNotContain("Corner Shop Plaintext")
    }

    @Test
    fun test_that_the_database_cannot_be_read_with_a_wrong_passphrase() = runTest {
        writeReceiptAndClose()

        assertThrows(Exception::class.java) {
            SQLiteDatabase.openDatabase(
                databaseFile.path,
                ByteArray(32) { 7 },
                null,
                SQLiteDatabase.OPEN_READONLY,
                null,
                null,
            ).use { db -> db.rawQuery("SELECT count(*) FROM receipts", null).use { it.moveToFirst() } }
        }
    }

    @Test
    fun test_that_the_database_reopens_with_the_same_passphrase() = runTest {
        writeReceiptAndClose(merchant = "Corner Shop")

        val reopened = ReceiptsDatabaseProvider(context, passphraseProvider, Dispatchers.IO, databaseName).get()

        assertThat(reopened.receiptDao().getCreatedAt(1)).isEqualTo(100)
        reopened.close()
    }

    private suspend fun writeReceiptAndClose(merchant: String = "Corner Shop") {
        val database = ReceiptsDatabaseProvider(context, passphraseProvider, Dispatchers.IO, databaseName).get()
        database.receiptDao().insert(
            ReceiptEntity(
                merchant = merchant,
                purchaseDate = 20_000,
                totalMinor = 450,
                currencyCode = "EUR",
                category = "other",
                note = null,
                createdAt = 100,
                updatedAt = 100,
            ),
        )
        database.close()
    }
}
