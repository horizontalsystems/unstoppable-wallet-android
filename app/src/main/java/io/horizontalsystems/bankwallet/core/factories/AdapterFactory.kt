package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.managers.BinanceKitManager
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Wallet

class AdapterFactory(
        private val context: Context,
        private val appConfigProvider: IAppConfigProvider,
        private val ethereumKitManager: IEthereumKitManager,
        private val eosKitManager: IEosKitManager,
        private val binanceKitManager: BinanceKitManager,
        private val feeRateProvider: IFeeRateProvider) {

    fun adapterForCoin(wallet: Wallet): IAdapter? {
        return when (val coinType = wallet.coin.type) {
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, appConfigProvider.testMode, feeRateProvider)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, appConfigProvider.testMode, feeRateProvider)
            is CoinType.Dash -> DashAdapter(wallet, appConfigProvider.testMode, feeRateProvider)
            is CoinType.Eos -> EosAdapter(wallet, coinType, eosKitManager.eosKit(wallet))
            is CoinType.Binance -> BinanceAdapter(wallet, binanceKitManager.binanceKit(wallet), coinType.symbol)
            is CoinType.Ethereum -> {
                EthereumAdapter(wallet, ethereumKitManager.ethereumKit(wallet), AddressParser("ethereum", true), feeRateProvider)
            }
            is CoinType.Erc20 -> {
                Erc20Adapter(context, wallet, ethereumKitManager.ethereumKit(wallet), coinType.decimal, coinType.fee, coinType.address, AddressParser("ethereum", true), feeRateProvider)
            }
        }
    }

    fun unlinkAdapter(adapter: IAdapter) {
        when (adapter) {
            is EthereumBaseAdapter -> {
                ethereumKitManager.unlink()
            }
            is EosAdapter -> {
                eosKitManager.unlink()
            }
            is BinanceAdapter -> {
                binanceKitManager.unlink()
            }
        }
    }
}
