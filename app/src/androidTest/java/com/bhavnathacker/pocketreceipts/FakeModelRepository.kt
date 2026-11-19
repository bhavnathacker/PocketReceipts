package com.bhavnathacker.pocketreceipts

import com.bhavnathacker.pocketreceipts.data.modeldownload.di.ModelRepositoryModule
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A [ModelRepository] whose state the test sets, so app tests never download the
 * real model. It starts [ModelState.Ready].
 */
@Singleton
class FakeModelRepository @Inject constructor() : ModelRepository {

    val state = MutableStateFlow<ModelState>(ModelState.Ready)
    var downloadsStarted = 0
        private set

    override fun monitorModelState(): Flow<ModelState> = state

    override suspend fun startDownload() {
        downloadsStarted++
    }

    override suspend fun markLoadVerified() {
        state.value = ModelState.Ready
    }

    override suspend fun modelPath(): String? = null
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ModelRepositoryModule::class])
abstract class FakeModelRepositoryModule {

    @Binds
    abstract fun bindModelRepository(repository: FakeModelRepository): ModelRepository
}
