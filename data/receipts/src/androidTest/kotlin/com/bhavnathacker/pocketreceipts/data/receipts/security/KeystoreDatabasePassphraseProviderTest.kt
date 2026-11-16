package com.bhavnathacker.pocketreceipts.data.receipts.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class KeystoreDatabasePassphraseProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alias = "test_${UUID.randomUUID()}"
    private lateinit var keyFile: File

    @Before
    fun setUp() {
        keyFile = File(context.noBackupFilesDir, "test_${UUID.randomUUID()}.key")
    }

    @After
    fun tearDown() {
        keyFile.delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test
    fun test_that_getPassphrase_returns_32_random_bytes() {
        val passphrase = KeystoreDatabasePassphraseProvider({ keyFile }, alias).getPassphrase()

        assertThat(passphrase).hasLength(32)
        assertThat(passphrase.toSet().size).isGreaterThan(1)
    }

    @Test
    fun test_that_getPassphrase_returns_the_same_passphrase_from_a_new_instance() {
        val first = KeystoreDatabasePassphraseProvider({ keyFile }, alias).getPassphrase()

        val second = KeystoreDatabasePassphraseProvider({ keyFile }, alias).getPassphrase()

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun test_that_the_key_file_does_not_contain_the_passphrase_in_plain_form() {
        val passphrase = KeystoreDatabasePassphraseProvider({ keyFile }, alias).getPassphrase()

        val stored = keyFile.readBytes()

        assertThat(stored.size).isGreaterThan(passphrase.size)
        assertThat(stored.asList().windowed(passphrase.size).none { it == passphrase.asList() }).isTrue()
    }

    @Test
    fun test_that_getPassphrase_throws_when_the_keystore_key_was_lost() {
        KeystoreDatabasePassphraseProvider({ keyFile }, alias).getPassphrase()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)

        assertThrows(GeneralSecurityException::class.java) {
            KeystoreDatabasePassphraseProvider({ keyFile }, alias).getPassphrase()
        }
    }
}
