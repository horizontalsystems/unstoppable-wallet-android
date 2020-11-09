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

    data class ViewItem(val coinName: String, val symbol: String, val decimal: Int)

    class InvalidAddress : Throwable()

    sealed class State {
        object Empty : State()
        object Loading : State()
        class ExistingCoin(val viewItem: ViewItem) : State()
        class Success(val viewItem: ViewItem) : State()
        class Failed(val error: Throwable) : State()
    }
}
