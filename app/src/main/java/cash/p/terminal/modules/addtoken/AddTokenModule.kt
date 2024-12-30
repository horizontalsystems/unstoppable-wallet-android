package cash.p.terminal.modules.addtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery

object AddTokenModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = AddTokenService(App.coinManager, App.walletManager, App.accountManager, App.marketKit)
            return AddTokenViewModel(service) as T
        }
    }

    interface IAddTokenBlockchainService {
        fun isValid(reference: String): Boolean
        fun tokenQuery(reference: String): TokenQuery
        suspend fun token(reference: String): Token
    }

}
