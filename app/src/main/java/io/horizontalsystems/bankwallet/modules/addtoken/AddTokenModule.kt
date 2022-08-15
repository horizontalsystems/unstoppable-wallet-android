package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery

object AddTokenModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val services = buildList {
                addAll(
                    App.evmBlockchainManager.allBlockchainTypes.map {
                        AddEvmTokenBlockchainService(it, App.networkManager)
                    }
                )
                add(AddBep2TokenBlockchainService(BlockchainType.BinanceChain, App.networkManager))
            }

            val service = AddTokenService(App.coinManager, services, App.walletManager, App.accountManager, App.marketKit)

            return AddTokenViewModel(service) as T
        }
    }

    interface IAddTokenBlockchainService {
        fun isValid(reference: String): Boolean
        fun tokenQuery(reference: String): TokenQuery
        suspend fun customCoin(reference: String): CustomCoin
    }

    data class CustomCoin(
        val tokenQuery: TokenQuery,
        val name: String,
        val code: String,
        val decimals: Int
    )
}
