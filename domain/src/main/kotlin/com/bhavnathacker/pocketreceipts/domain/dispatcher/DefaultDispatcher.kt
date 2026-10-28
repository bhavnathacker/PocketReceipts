package com.bhavnathacker.pocketreceipts.domain.dispatcher

import javax.inject.Qualifier

/**
 * Qualifies the [kotlinx.coroutines.CoroutineDispatcher] for CPU-bound work
 * (parsing, sorting, mapping large lists). Inject it instead of using
 * `Dispatchers.Default` directly, so tests can substitute a test dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
