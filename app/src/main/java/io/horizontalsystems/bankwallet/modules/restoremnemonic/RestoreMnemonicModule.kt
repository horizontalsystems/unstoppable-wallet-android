package io.horizontalsystems.bankwallet.modules.restoremnemonic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object RestoreMnemonicModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreMnemonicService(App.wordsManager)

            return RestoreMnemonicViewModel(service, listOf(service)) as T
        }
    }

}
