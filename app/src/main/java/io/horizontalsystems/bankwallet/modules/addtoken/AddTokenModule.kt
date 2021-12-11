package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.marketkit.models.PlatformCoin

object AddTokenModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ethereumService =
                AddEvmTokenBlockchainService(AddEvmTokenBlockchainService.Blockchain.Ethereum, App.networkManager)
            val binanceSmartChainService = AddEvmTokenBlockchainService(
                AddEvmTokenBlockchainService.Blockchain.BinanceSmartChain,
                App.networkManager
            )
            val binanceService = AddBep2TokenBlockchainService(App.networkManager)
            val services = listOf(ethereumService, binanceSmartChainService, binanceService)

            val service = AddTokenService(App.coinManager, services, App.walletManager, App.accountManager)
            val viewModel = AddTokenViewModel(service)

            return viewModel as T
        }
    }

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
        class Fetched(val customTokens: List<CustomToken>) : State()
        class Failed(val error: Throwable) : State()
    }
}
