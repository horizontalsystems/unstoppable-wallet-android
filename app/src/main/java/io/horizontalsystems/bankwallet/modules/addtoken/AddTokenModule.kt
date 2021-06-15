package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.addtoken.bep2.AddBep2TokenBlockchainService
import io.horizontalsystems.coinkit.models.Coin
import kotlinx.android.parcel.Parcelize

object AddTokenModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val activeAccount = App.accountManager.activeAccount!!
            val erc20NetworkType = App.accountSettingManager.ethereumNetwork(activeAccount).networkType
            val bep20NetworkType = App.accountSettingManager.binanceSmartChainNetwork(activeAccount).networkType
            val evmService = AddEvmTokenBlockchainService(App.appConfigProvider, App.networkManager)
            val bep2Service = AddBep2TokenBlockchainService(App.buildConfigProvider)
            val service = AddTokenService(App.coinManager, evmService, bep2Service, App.walletManager, App.accountManager, erc20NetworkType, bep20NetworkType)
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
