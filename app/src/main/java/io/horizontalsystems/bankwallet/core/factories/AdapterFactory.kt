package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.IEosKitManager
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.managers.BinanceKitManager
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.IAppConfigTestMode

class AdapterFactory(
        private val context: Context,
        private val appConfigProvider: IAppConfigTestMode,
        private val ethereumKitManager: IEthereumKitManager,
        private val eosKitManager: IEosKitManager,
        private val binanceKitManager: BinanceKitManager,
        private val blockchainSettingManager: IBlockchainSettingsManager) {

    fun adapter(wallet: Wallet): IAdapter? {
        val settings = blockchainSettingManager.blockchainSettings(wallet.coin.type)
        return when (val coinType = wallet.coin.type) {
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, settings, appConfigProvider.testMode)
            is CoinType.Litecoin -> LitecoinAdapter(wallet, settings, appConfigProvider.testMode)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, settings, appConfigProvider.testMode)
            is CoinType.Dash -> DashAdapter(wallet, settings, appConfigProvider.testMode)
            is CoinType.Eos -> EosAdapter(coinType, eosKitManager.eosKit(wallet), wallet.coin.decimal)
            is CoinType.Binance -> BinanceAdapter(binanceKitManager.binanceKit(wallet), coinType.symbol)
            is CoinType.Ethereum -> EthereumAdapter(ethereumKitManager.ethereumKit(wallet))
            is CoinType.Erc20 -> Erc20Adapter(context, ethereumKitManager.ethereumKit(wallet), wallet.coin.decimal, coinType.fee, coinType.address, coinType.gasLimit, coinType.minimumRequiredBalance, coinType.minimumSendAmount)
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
