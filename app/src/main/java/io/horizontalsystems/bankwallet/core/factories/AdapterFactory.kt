package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(private val appConfigProvider: IAppConfigProvider, private val localStorage: ILocalStorage, private val ethereumKitManager: IEthereumKitManager) {

    fun adapterForCoin(coin: Coin, authData: AuthData): IAdapter? = when (coin.type) {
        is CoinType.Bitcoin -> {
            BitcoinAdapter(coin, authData, localStorage.isNewWallet, appConfigProvider.testMode)
        }
        is CoinType.BitcoinCash -> {
            BitcoinAdapter(coin, authData, localStorage.isNewWallet, appConfigProvider.testMode)
        }
        is CoinType.Ethereum -> {
            EthereumAdapter.adapter(coin, ethereumKitManager.ethereumKit(authData))
        }
        is CoinType.Erc20 -> {
            Erc20Adapter.adapter(coin, ethereumKitManager.ethereumKit(authData), coin.type.address, coin.type.decimal)
        }
    }

    fun unlinkAdapter(adapter: IAdapter) {
        when (adapter) {
            is EthereumBaseAdapter -> {
                ethereumKitManager.unlink()
            }
        }
    }
}
