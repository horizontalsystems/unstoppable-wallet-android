package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.Coin
import kotlinx.android.parcel.Parcelize

object AddTokenModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val activeAccount = App.accountManager.activeAccount!!
            val erc20NetworkType = App.accountSettingManager.ethereumNetwork(activeAccount).networkType
            val bep20NetworkType = App.accountSettingManager.binanceSmartChainNetwork(activeAccount).networkType

            val ethereumService = AddEvmTokenBlockchainService(App.appConfigProvider, App.networkManager, erc20NetworkType)
            val binanceSmartChainService = AddEvmTokenBlockchainService(App.appConfigProvider, App.networkManager, bep20NetworkType)
            val binanceService = AddBep2TokenBlockchainService(App.buildConfigProvider)
            val services = listOf(ethereumService, binanceSmartChainService, binanceService)

            val service = AddTokenService(App.coinManager, services, App.walletManager, App.accountManager)
            val viewModel = AddTokenViewModel(service)

            return viewModel as T
        }
    }

    data class ViewItem(val coinType: String?, val coinName: String, val symbol: String, val decimal: Int)

    sealed class State {
        object Idle : State()
        object Loading : State()
        class AlreadyExists(val coin: Coin) : State()
        class Fetched(val coin: Coin) : State()
        class Failed(val error: Throwable) : State()
    }
}

@Parcelize
enum class TokenType(val value: String) : Parcelable {
    Erc20("erc20"),
    Bep20("bep20"),
    Bep2("bep2");
}
