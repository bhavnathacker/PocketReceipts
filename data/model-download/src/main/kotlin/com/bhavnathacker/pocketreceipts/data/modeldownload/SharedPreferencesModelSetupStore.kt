package com.bhavnathacker.pocketreceipts.data.modeldownload

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** [ModelSetupStore] in the module's own private [android.content.SharedPreferences]. */
@Singleton
internal class SharedPreferencesModelSetupStore @Inject constructor(
    @ApplicationContext context: Context,
) : ModelSetupStore {

    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override var downloadId: Long?
        get() = preferences.getLong(KEY_DOWNLOAD_ID, NO_ID).takeIf { it != NO_ID }
        set(value) = preferences.edit { if (value == null) remove(KEY_DOWNLOAD_ID) else putLong(KEY_DOWNLOAD_ID, value) }

    override var loadVerified: Boolean
        get() = preferences.getBoolean(KEY_LOAD_VERIFIED, false)
        set(value) = preferences.edit { putBoolean(KEY_LOAD_VERIFIED, value) }

    override var failure: SetupFailure?
        get() = preferences.getString(KEY_FAILURE, null)?.let { name ->
            SetupFailure.entries.firstOrNull { it.name == name }
        }
        set(value) = preferences.edit { if (value == null) remove(KEY_FAILURE) else putString(KEY_FAILURE, value.name) }

    private companion object {
        const val FILE_NAME = "model_setup"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_LOAD_VERIFIED = "load_verified"
        const val KEY_FAILURE = "failure"
        const val NO_ID = -1L
    }
}
