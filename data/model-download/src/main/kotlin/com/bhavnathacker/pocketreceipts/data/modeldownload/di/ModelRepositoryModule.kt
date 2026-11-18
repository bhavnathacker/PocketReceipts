package com.bhavnathacker.pocketreceipts.data.modeldownload.di

import com.bhavnathacker.pocketreceipts.data.modeldownload.repository.DefaultModelRepository
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds [ModelRepository]. Public, unlike the module's other bindings, so the
 * app's instrumented tests can replace it with a fake (`@TestInstallIn`) instead
 * of downloading the real model.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModelRepositoryModule {

    @Binds
    internal abstract fun bindModelRepository(repository: DefaultModelRepository): ModelRepository
}
