package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(
        private val appConfigProvider: IAppConfigProvider,
        private val localStorage: ILocalStorage,
        private val ethereumKitManager: IEthereumKitManager,
        private val feeRateProvider: IFeeRateProvider) {

    fun adapterForCoin(coin: Coin, authData: AuthData): IAdapter? = when (coin.type) {
        is CoinType.Bitcoin -> BitcoinAdapter(coin, authData, localStorage.isNewWallet, appConfigProvider.testMode, feeRateProvider)
        is CoinType.BitcoinCash -> BitcoinCashAdapter(coin, authData, localStorage.isNewWallet, appConfigProvider.testMode, feeRateProvider)
        is CoinType.Ethereum -> EthereumAdapter(coin, ethereumKitManager.ethereumKit(authData), feeRateProvider)
        is CoinType.Erc20 -> Erc20Adapter(coin, ethereumKitManager.ethereumKit(authData), coin.type.decimal, coin.type.address, feeRateProvider)
    }

    fun unlinkAdapter(adapter: IAdapter) {
        when (adapter) {
            is EthereumBaseAdapter -> {
                ethereumKitManager.unlink()
            }
        }
    }
}
