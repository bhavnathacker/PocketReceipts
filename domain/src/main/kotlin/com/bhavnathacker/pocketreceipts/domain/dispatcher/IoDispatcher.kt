package com.bhavnathacker.pocketreceipts.domain.dispatcher

import javax.inject.Qualifier

/**
 * Qualifies the [kotlinx.coroutines.CoroutineDispatcher] for blocking I/O
 * (database, files, network). Inject it instead of using `Dispatchers.IO`
 * directly, so tests can substitute a test dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
