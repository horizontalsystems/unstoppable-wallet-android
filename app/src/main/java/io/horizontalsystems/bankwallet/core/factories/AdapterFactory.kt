package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(
        private val context: Context,
        private val appConfigProvider: IAppConfigProvider,
        private val localStorage: ILocalStorage,
        private val ethereumKitManager: IEthereumKitManager,
        private val feeRateProvider: IFeeRateProvider) {

    fun adapterForCoin(wallet: Wallet, authData: AuthData): IAdapter? = when (wallet.coin.type) {
        is CoinType.Bitcoin -> BitcoinAdapter(wallet, authData, localStorage.syncMode, appConfigProvider.testMode, feeRateProvider)
        is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, authData, localStorage.syncMode, appConfigProvider.testMode, feeRateProvider)
        is CoinType.Dash -> DashAdapter(wallet, authData, localStorage.syncMode, appConfigProvider.testMode, feeRateProvider)
        is CoinType.Ethereum -> {
            val addressParser = AddressParser("ethereum", true)
            EthereumAdapter(wallet, ethereumKitManager.ethereumKit(authData), addressParser, feeRateProvider)
        }
        is CoinType.Erc20 -> {
            val addressParser = AddressParser("ethereum", true)
            Erc20Adapter(context, wallet, ethereumKitManager.ethereumKit(authData), wallet.coin.type.decimal, wallet.coin.type.fee, wallet.coin.type.address, addressParser, feeRateProvider)
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
