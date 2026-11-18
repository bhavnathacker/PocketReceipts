package com.bhavnathacker.pocketreceipts.data.modeldownload

import android.os.StatFs
import java.io.File
import javax.inject.Inject

/** Free storage on the volume that holds a directory. */
internal fun interface StorageInfo {
    fun freeBytes(directory: File): Long
}

internal class StatFsStorageInfo @Inject constructor() : StorageInfo {
    override fun freeBytes(directory: File): Long = StatFs(directory.path).availableBytes
}
