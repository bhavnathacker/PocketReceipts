package com.bhavnathacker.pocketreceipts.data.ocr.di

import com.bhavnathacker.pocketreceipts.data.ocr.repository.FileScanImageRepository
import com.bhavnathacker.pocketreceipts.data.ocr.repository.MlKitTextRecognitionRepository
import com.bhavnathacker.pocketreceipts.domain.scan.ScanImageRepository
import com.bhavnathacker.pocketreceipts.domain.scan.TextRecognitionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OcrDataModule {

    @Binds
    abstract fun bindTextRecognitionRepository(
        repository: MlKitTextRecognitionRepository,
    ): TextRecognitionRepository

    @Binds
    abstract fun bindScanImageRepository(repository: FileScanImageRepository): ScanImageRepository
}
