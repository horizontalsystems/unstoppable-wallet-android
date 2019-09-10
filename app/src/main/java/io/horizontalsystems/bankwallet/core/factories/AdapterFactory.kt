package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEosKitManager
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.managers.BinanceKitManager
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Wallet

class AdapterFactory(
        private val context: Context,
        private val appConfigProvider: IAppConfigProvider,
        private val ethereumKitManager: IEthereumKitManager,
        private val eosKitManager: IEosKitManager,
        private val binanceKitManager: BinanceKitManager) {

    fun adapter(wallet: Wallet): IAdapter? {
        val confirmationsThreshold = appConfigProvider.confirmationsThreshold(wallet.coin.type)

        return when (val coinType = wallet.coin.type) {
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, appConfigProvider.testMode, confirmationsThreshold)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, appConfigProvider.testMode, confirmationsThreshold)
            is CoinType.Dash -> DashAdapter(wallet, appConfigProvider.testMode, confirmationsThreshold)
            is CoinType.Eos -> EosAdapter(coinType, eosKitManager.eosKit(wallet), wallet.coin.decimal, confirmationsThreshold)
            is CoinType.Binance -> BinanceAdapter(binanceKitManager.binanceKit(wallet), coinType.symbol, confirmationsThreshold)
            is CoinType.Ethereum -> EthereumAdapter(ethereumKitManager.ethereumKit(wallet), confirmationsThreshold)
            is CoinType.Erc20 -> Erc20Adapter(context, ethereumKitManager.ethereumKit(wallet), wallet.coin.decimal, coinType.fee, coinType.address, confirmationsThreshold)
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
