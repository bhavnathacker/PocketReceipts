package com.bhavnathacker.pocketreceipts.data.modeldownload.di

import android.content.Context
import com.bhavnathacker.pocketreceipts.data.modeldownload.AndroidModelDownloader
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelDownloader
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelSetupStore
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelSpec
import com.bhavnathacker.pocketreceipts.data.modeldownload.SharedPreferencesModelSetupStore
import com.bhavnathacker.pocketreceipts.data.modeldownload.StatFsStorageInfo
import com.bhavnathacker.pocketreceipts.data.modeldownload.StorageInfo
import com.bhavnathacker.pocketreceipts.data.modeldownload.repository.DefaultModelRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ModelDownloadDataModule {

    @Binds
    abstract fun bindModelDownloader(downloader: AndroidModelDownloader): ModelDownloader

    @Binds
    abstract fun bindModelSetupStore(store: SharedPreferencesModelSetupStore): ModelSetupStore

    @Binds
    abstract fun bindStorageInfo(storageInfo: StatFsStorageInfo): StorageInfo

    companion object {
        @Provides
        fun provideModelSpec(): ModelSpec = ModelSpec.GEMMA_4_E2B

        /**
         * App-specific external storage (`Android/data/<app>/files/models`): DownloadManager
         * can write there, it is removed on uninstall, and the backup rules exclude it.
         */
        @Provides
        @Named(DefaultModelRepository.MODEL_DIRECTORY)
        fun provideModelDirectory(@ApplicationContext context: Context): File =
            context.getExternalFilesDir(MODELS) ?: File(context.filesDir, MODELS)

        private const val MODELS = "models"
    }
}
