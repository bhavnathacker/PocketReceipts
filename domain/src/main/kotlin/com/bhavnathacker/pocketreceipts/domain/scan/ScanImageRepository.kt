package com.bhavnathacker.pocketreceipts.domain.scan

/**
 * Removes scanned receipt images. The app never keeps them.
 */
interface ScanImageRepository {

    /**
     * Deletes the scanned image at [imageUri]. Does nothing if it is already gone
     * or isn't a scan the app owns.
     */
    suspend fun delete(imageUri: String)
}
