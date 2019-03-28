package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(private val appConfigProvider: IAppConfigProvider, private val localStorage: ILocalStorage, private val ethereumKitManager: IEthereumKitManager) {

    fun adapterForCoin(coin: Coin, authData: AuthData): IAdapter? = when (coin.type) {
        is CoinType.Bitcoin -> {
            val addressParser = AddressParser("bitcoin", true)
            BitcoinAdapter(coin, authData, localStorage.isNewWallet, addressParser, appConfigProvider.testMode)
        }
        is CoinType.BitcoinCash -> {
            val addressParser = AddressParser("bitcoincash", false)
            BitcoinAdapter(coin, authData, localStorage.isNewWallet, addressParser, appConfigProvider.testMode)
        }
        is CoinType.Ethereum -> {
            val addressParser = AddressParser("ethereum", true)
            EthereumAdapter.adapter(coin, ethereumKitManager.ethereumKit(authData), addressParser)
        }
        is CoinType.Erc20 -> {
            val addressParser = AddressParser("ethereum", true)
            Erc20Adapter.adapter(coin, ethereumKitManager.ethereumKit(authData), coin.type.address, coin.type.decimal, addressParser)
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
