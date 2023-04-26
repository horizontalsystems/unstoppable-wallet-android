package io.horizontalsystems.bankwallet.modules.swap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ErrorShareService {

    private val _errorsStateFlow = MutableStateFlow<List<Throwable>>(emptyList())
    val errorsStateFlow: StateFlow<List<Throwable>>
        get() = _errorsStateFlow

    fun updateErrors(errors: List<Throwable>) {
        _errorsStateFlow.update { errors }
    }
}
