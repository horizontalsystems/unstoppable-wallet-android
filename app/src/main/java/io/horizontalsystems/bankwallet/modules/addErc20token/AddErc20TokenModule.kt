package io.horizontalsystems.bankwallet.modules.addErc20token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object AddErc20TokenModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = AddErc20TokenViewModel(App.coinManager, App.erc20ContractInfoProvider)
            return viewModel as T
        }
    }
}
