package com.bhavnathacker.pocketreceipts.data.ai.di

import com.bhavnathacker.pocketreceipts.data.ai.AppVisibility
import com.bhavnathacker.pocketreceipts.data.ai.EngineFactory
import com.bhavnathacker.pocketreceipts.data.ai.InferenceDispatcher
import com.bhavnathacker.pocketreceipts.data.ai.LiteRtEngineFactory
import com.bhavnathacker.pocketreceipts.data.ai.ProcessAppVisibility
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

    @Binds
    abstract fun bindEngineFactory(factory: LiteRtEngineFactory): EngineFactory

    @Binds
    abstract fun bindAppVisibility(visibility: ProcessAppVisibility): AppVisibility

    companion object {
        @Provides
        @Singleton
        @InferenceDispatcher
        fun provideInferenceDispatcher(): CoroutineDispatcher =
            Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "litert-lm") }.asCoroutineDispatcher()
    }
}
