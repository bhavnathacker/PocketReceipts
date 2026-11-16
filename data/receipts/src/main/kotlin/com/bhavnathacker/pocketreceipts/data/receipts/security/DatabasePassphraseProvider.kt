package com.bhavnathacker.pocketreceipts.data.receipts.security

/**
 * Supplies the passphrase that encrypts the receipts database.
 */
internal interface DatabasePassphraseProvider {

    /**
     * The passphrase, created on first use and the same on every later call on
     * this device. Blocking: call it off the main thread.
     *
     * @throws java.security.GeneralSecurityException if the stored passphrase
     * can't be unwrapped, for example because the Keystore key was lost.
     */
    fun getPassphrase(): ByteArray
}
