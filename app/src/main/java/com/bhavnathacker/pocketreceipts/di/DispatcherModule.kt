package com.bhavnathacker.pocketreceipts.di

import com.bhavnathacker.pocketreceipts.domain.dispatcher.DefaultDispatcher
import com.bhavnathacker.pocketreceipts.domain.dispatcher.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Provides the coroutine dispatchers qualified in `:domain`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
