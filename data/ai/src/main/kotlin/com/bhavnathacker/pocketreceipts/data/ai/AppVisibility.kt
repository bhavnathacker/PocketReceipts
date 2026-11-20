package com.bhavnathacker.pocketreceipts.data.ai

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Whether the app is in the foreground. */
internal interface AppVisibility {

    /** True while one of the app's activities is started; emits the current value first. */
    val isInForeground: Flow<Boolean>
}

/**
 * Reads the app's foreground state from [ProcessLifecycleOwner]. Note that another app's
 * activity on top of ours (such as the ML Kit document scanner) counts as background.
 */
internal class ProcessAppVisibility @Inject constructor() : AppVisibility {

    override val isInForeground: Flow<Boolean> =
        ProcessLifecycleOwner.get().lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .distinctUntilChanged()
}
