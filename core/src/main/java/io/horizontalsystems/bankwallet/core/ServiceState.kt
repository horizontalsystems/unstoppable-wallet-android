package io.horizontalsystems.bankwallet.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class ServiceState<T> {

    private val _stateFlow by lazy {
        MutableStateFlow(createState())
    }

    val stateFlow: StateFlow<T>
        get() = _stateFlow.asStateFlow()

    protected abstract fun createState() : T

    protected fun emitState() {
        _stateFlow.update {
            createState()
        }
    }
}
