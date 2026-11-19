package com.bhavnathacker.pocketreceipts.data.ai

import javax.inject.Qualifier

/** The single thread all LiteRT-LM engine work runs on. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class InferenceDispatcher
