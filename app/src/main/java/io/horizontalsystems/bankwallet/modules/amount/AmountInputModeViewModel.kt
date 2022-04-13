package io.horizontalsystems.bankwallet.modules.amount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage

class AmountInputModeViewModel(private val localStorage: ILocalStorage) : ViewModel() {

    var inputType by mutableStateOf(localStorage.sendInputType ?: AmountInputModule.InputType.COIN)
        private set

    fun onToggleInputType() {
        inputType = inputType.reversed()
        localStorage.sendInputType = inputType
    }
}

