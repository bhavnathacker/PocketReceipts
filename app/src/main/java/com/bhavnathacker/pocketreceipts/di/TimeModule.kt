package com.bhavnathacker.pocketreceipts.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * Provides the [Clock] used for "today" and record timestamps. Unscoped, so a
 * time-zone change is picked up by objects created afterwards.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
