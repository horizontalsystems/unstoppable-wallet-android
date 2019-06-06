package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(
        private val context: Context,
        private val appConfigProvider: IAppConfigProvider,
        private val localStorage: ILocalStorage,
        private val ethereumKitManager: IEthereumKitManager,
        private val feeRateProvider: IFeeRateProvider) {

    fun adapterForCoin(coin: Coin, authData: AuthData): IAdapter? = when (coin.type) {
        is CoinType.Bitcoin -> BitcoinAdapter(coin, authData, localStorage.syncMode, appConfigProvider.testMode, feeRateProvider)
        is CoinType.BitcoinCash -> BitcoinCashAdapter(coin, authData, localStorage.syncMode, appConfigProvider.testMode, feeRateProvider)
        is CoinType.Dash -> DashAdapter(coin, authData, localStorage.syncMode, appConfigProvider.testMode, feeRateProvider)
        is CoinType.Ethereum -> {
            val addressParser = AddressParser("ethereum", true)
            EthereumAdapter(coin, ethereumKitManager.ethereumKit(authData), addressParser, feeRateProvider)
        }
        is CoinType.Erc20 -> {
            val addressParser = AddressParser("ethereum", true)
            Erc20Adapter(context, coin, ethereumKitManager.ethereumKit(authData), coin.type.decimal, coin.type.fee, coin.type.address, addressParser, feeRateProvider)
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
