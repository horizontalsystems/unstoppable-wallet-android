package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoins.*
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable

object ManageWalletsModule {

    interface IManageWalletsService {
        val state: State
        val stateAsync: Observable<State>
        fun enable(coin: Coin, derivationSetting: DerivationSetting? = null)
        fun disable(coin: Coin)
        fun storeCoinToEnable(coin: Coin)
        fun account(coin: Coin): Account?
    }

    data class State(val featuredItems: List<Item>, val items: List<Item>) {
        fun item(coin: Coin): Item? {
            val allItems = featuredItems + items
            return allItems.find { it.coin == coin }
        }

        companion object {
            fun empty(): State {
                return State(listOf(), listOf())
            }
        }
    }

    class Item(val coin: Coin, var state: ItemState)

    sealed class ItemState {
        object NoAccount : ItemState()
        class HasAccount(val account: Account, val hasWallet: Boolean) : ItemState()
    }

    class Factory : ViewModelProvider.Factory {

        private val enableCoinsService by lazy {
            EnableCoinsService(
                    App.buildConfigProvider,
                    EnableCoinsErc20Provider(App.networkManager),
                    EnableCoinsBep2Provider(App.buildConfigProvider),
                    EnableCoinsBep20Provider(App.networkManager,App.appConfigProvider.bscscanApiKey),
                    App.coinManager
            )
        }

        private val blockchainSettingsService by lazy {
            BlockchainSettingsService(App.derivationSettingsManager, App.bitcoinCashCoinTypeManager)
        }

        private val manageWalletsService by lazy {
            ManageWalletsService(App.coinManager, App.walletManager, App.accountManager, enableCoinsService, blockchainSettingsService)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                ManageWalletsViewModel::class.java -> {
                    ManageWalletsViewModel(manageWalletsService, blockchainSettingsService, listOf(manageWalletsService)) as T
                }
                BlockchainSettingsViewModel::class.java -> {
                    BlockchainSettingsViewModel(blockchainSettingsService, StringProvider()) as T
                }
                EnableCoinsViewModel::class.java -> {
                    EnableCoinsViewModel(enableCoinsService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
