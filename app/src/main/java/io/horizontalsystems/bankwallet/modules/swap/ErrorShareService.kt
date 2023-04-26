package cash.p.terminal.modules.swap
>>>>>>>> e3363e417 (Rename swap package name):app/src/main/java/cash.p.terminal/modules/swap/ErrorShareService.kt

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
