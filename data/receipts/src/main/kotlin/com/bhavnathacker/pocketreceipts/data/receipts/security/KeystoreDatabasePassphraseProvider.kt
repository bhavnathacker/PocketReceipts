package com.bhavnathacker.pocketreceipts.data.receipts.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps a random database passphrase in [keyFile], wrapped (AES-256-GCM) by a
 * key that never leaves the Android Keystore. The key needs no user
 * authentication, so the app opens without prompts. The file from [keyFile] is
 * expected to live in `noBackupFilesDir`, so neither it nor the device-bound key
 * is backed up. [keyFile] is resolved on first use, not on construction, to keep
 * disk access off the injection path.
 *
 * File format: `[iv length: 1 byte][iv][ciphertext + GCM tag]`.
 */
internal class KeystoreDatabasePassphraseProvider(
    keyFile: () -> File,
    private val keyAlias: String,
) : DatabasePassphraseProvider {

    private val keyFile: File by lazy(keyFile)

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    @Synchronized
    override fun getPassphrase(): ByteArray {
        val wrappingKey = getOrCreateWrappingKey()
        return if (keyFile.exists()) {
            unwrap(keyFile.readBytes(), wrappingKey)
        } else {
            createPassphrase(wrappingKey)
        }
    }

    private fun createPassphrase(wrappingKey: SecretKey): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, wrappingKey)
        }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(passphrase)
        val content = ByteBuffer.allocate(1 + iv.size + ciphertext.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()
        writeAtomically(content)
        return passphrase
    }

    private fun unwrap(content: ByteArray, wrappingKey: SecretKey): ByteArray {
        val buffer = ByteBuffer.wrap(content)
        val iv = ByteArray(buffer.get().toInt()).also { buffer.get(it) }
        val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(ciphertext)
    }

    private fun writeAtomically(content: ByteArray) {
        keyFile.parentFile?.mkdirs()
        val temp = File(keyFile.parentFile, "${keyFile.name}.tmp")
        temp.writeBytes(content)
        check(temp.renameTo(keyFile)) { "Could not store the database key file" }
    }

    private fun getOrCreateWrappingKey(): SecretKey =
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: generateWrappingKey()

    private fun generateWrappingKey(): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateWrappingKey(strongBox = true)
            } catch (e: StrongBoxUnavailableException) {
                Timber.i("StrongBox unavailable, using the default Keystore")
            }
        }
        return generateWrappingKey(strongBox = false)
    }

    private fun generateWrappingKey(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(WRAPPING_KEY_BITS)
            .apply { if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setIsStrongBoxBacked(true) }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    companion object {
        const val KEY_FILE_NAME = "receipts_db.key"
        const val KEY_ALIAS = "receipts_db_key_wrapper"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val WRAPPING_KEY_BITS = 256
        private const val PASSPHRASE_BYTES = 32
    }
}
