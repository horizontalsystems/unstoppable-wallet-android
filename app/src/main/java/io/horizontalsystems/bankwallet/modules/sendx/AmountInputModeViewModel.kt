package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.send.SendModule

class AmountInputModeViewModel(private val localStorage: ILocalStorage) : ViewModel() {

    var inputType by mutableStateOf(localStorage.sendInputType ?: SendModule.InputType.COIN)
        private set

    fun onToggleInputType() {
        inputType = inputType.reversed()
        localStorage.sendInputType = inputType
    }
}

object AmountInputModeModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AmountInputModeViewModel(App.localStorage) as T
        }
    }
}
