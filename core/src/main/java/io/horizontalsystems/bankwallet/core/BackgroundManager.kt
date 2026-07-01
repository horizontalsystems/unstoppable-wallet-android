package io.horizontalsystems.bankwallet.core

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class BackgroundManager : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _stateFlow = MutableSharedFlow<BackgroundManagerState>(
        replay = 1,
        extraBufferCapacity = 2
    )
    val stateFlow: SharedFlow<BackgroundManagerState> = _stateFlow

    private var lastState: BackgroundManagerState? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        emitState(BackgroundManagerState.EnterForeground)
    }

    override fun onStop(owner: LifecycleOwner) {
        emitState(BackgroundManagerState.EnterBackground)
    }

    private fun emitState(state: BackgroundManagerState) {
        // Avoid duplicate emissions
        if (lastState == state) return

        lastState = state
        scope.launch {
            _stateFlow.emit(state)
        }
    }

}

enum class BackgroundManagerState {
    EnterForeground,
    EnterBackground,
}