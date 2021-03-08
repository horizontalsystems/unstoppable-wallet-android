package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.addtoken.bep2.AddBep2TokenBlockchainService
import io.horizontalsystems.bankwallet.modules.addtoken.bep20.AddBep20TokenResolver
import io.horizontalsystems.bankwallet.modules.addtoken.erc20.AddErc20TokenResolver
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import kotlinx.android.parcel.Parcelize

object AddTokenModule {
    class Factory(private val tokenType: TokenType) : ViewModelProvider.Factory {
        private val stringProvider by lazy { StringProvider() }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = when (tokenType) {
                TokenType.Erc20 -> {
                    val resolver = AddErc20TokenResolver(App.buildConfigProvider.testMode, App.appConfigProvider.etherscanApiKey)
                    val blockchainService = AddEvmTokenBlockchainService(resolver, App.networkManager)
                    val service = AddTokenService(App.coinManager, blockchainService, App.walletManager, App.accountManager)
                    AddTokenViewModel(service, stringProvider, R.string.AddErc20Token_Title, R.string.AddEvmToken_ContractAddressHint)
                }
                TokenType.Bep20 -> {
                    val resolver = AddBep20TokenResolver(App.buildConfigProvider.testMode, App.appConfigProvider.bscscanApiKey)
                    val blockchainService = AddEvmTokenBlockchainService(resolver, App.networkManager)
                    val service = AddTokenService(App.coinManager, blockchainService, App.walletManager, App.accountManager)
                    AddTokenViewModel(service, stringProvider, R.string.AddBep20Token_Title, R.string.AddEvmToken_ContractAddressHint)
                }
                TokenType.Bep2 -> {
                    val blockchainService = AddBep2TokenBlockchainService(App.buildConfigProvider)
                    val service = AddTokenService(App.coinManager, blockchainService, App.walletManager, App.accountManager)
                    AddTokenViewModel(service, stringProvider, R.string.AddBep2Token_Title, R.string.AddBep2Token_TokenSymbolHint)
                }
            }
            return viewModel as T
        }
    }

    data class ViewItem(val coinName: String, val symbol: String, val decimal: Int)

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

interface IAddEvmTokenResolver {
    val apiUrl: String
    val explorerKey: String
    fun coinType(address: String): CoinType
}
