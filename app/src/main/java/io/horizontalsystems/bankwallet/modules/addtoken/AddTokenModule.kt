package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

object AddTokenModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val services = buildList {
                addAll(
                    App.evmBlockchainManager.allBlockchains.map {
                        AddEvmTokenBlockchainService(it, App.networkManager)
                    }
                )
                App.marketKit.blockchain(BlockchainType.BinanceChain.uid)?.let {
                    add(AddBep2TokenBlockchainService(it, App.networkManager))
                }
            }

            val service = AddTokenService(App.coinManager, services, App.walletManager, App.accountManager)

            return AddTokenViewModel(service) as T
        }
    }

    interface IAddTokenBlockchainService {
        fun isValid(reference: String): Boolean
        fun tokenQuery(reference: String): TokenQuery
        suspend fun token(reference: String): Token
    }

}
