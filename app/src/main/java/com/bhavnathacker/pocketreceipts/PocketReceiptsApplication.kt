package com.bhavnathacker.pocketreceipts

import android.app.Application
import timber.log.Timber

class PocketReceiptsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
