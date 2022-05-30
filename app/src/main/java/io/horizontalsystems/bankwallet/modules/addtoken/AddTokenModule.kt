package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Single

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
                add(AddBep2TokenBlockchainService(App.networkManager))
            }

            val service = AddTokenService(App.coinManager, services, App.walletManager, App.accountManager)

            return AddTokenViewModel(service) as T
        }
    }

    interface IAddTokenBlockchainService {
        fun isValid(reference: String): Boolean
        fun coinType(reference: String): CoinType
        fun customCoinsSingle(reference: String): Single<CustomCoin>
    }

    data class CustomCoin(
        val type: CoinType,
        val name: String,
        val code: String,
        val decimals: Int
    )

    data class ViewItem(
        val coinType: String,
        val coinName: String?,
        val symbol: String?,
        val decimals: Int?
    )

    sealed class State {
        object Idle : State()
        object Loading : State()
        class AlreadyExists(val platformCoins: List<PlatformCoin>) : State()
        class Fetched(val customCoins: List<CustomCoin>) : State()
        class Failed(val error: Throwable) : State()
    }
}
