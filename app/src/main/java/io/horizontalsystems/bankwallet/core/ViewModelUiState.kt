package io.horizontalsystems.bankwallet.core

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class ViewModelUiState<T> : ViewModel() {

    private val _uiState by lazy {
        mutableStateOf(createState())
    }

    val uiState: T
        get() = _uiState.value

    protected abstract fun createState() : T

    protected fun emitState() {
        viewModelScope.launch {
            _uiState.value = createState()
        }
    }
}
