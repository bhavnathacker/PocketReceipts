package com.bhavnathacker.pocketreceipts.data.ai.di

import com.bhavnathacker.pocketreceipts.data.ai.InferenceDispatcher
import com.bhavnathacker.pocketreceipts.data.ai.repository.LiteRtModelRuntimeRepository
import com.bhavnathacker.pocketreceipts.data.ai.repository.LiteRtReceiptExtractionRepository
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRuntimeRepository
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptExtractionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiDataModule {

    @Binds
    abstract fun bindReceiptExtractionRepository(
        repository: LiteRtReceiptExtractionRepository,
    ): ReceiptExtractionRepository

    @Binds
    abstract fun bindModelRuntimeRepository(repository: LiteRtModelRuntimeRepository): ModelRuntimeRepository

    companion object {
        @Provides
        @Singleton
        @InferenceDispatcher
        fun provideInferenceDispatcher(): CoroutineDispatcher =
            Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "litert-lm") }.asCoroutineDispatcher()
    }
}
