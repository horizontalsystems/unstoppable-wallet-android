package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
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
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val syncModeSettingsManager: ISyncModeSettingsManager) {

    fun adapter(wallet: Wallet): IAdapter? {
        val derivation = derivationSettingsManager.derivationSetting(wallet.coin.type)?.derivation
        val syncMode = syncModeSettingsManager.syncModeSetting(wallet.coin.type)?.syncMode

        return when (val coinType = wallet.coin.type) {
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, derivation, syncMode, appConfigProvider.testMode)
            is CoinType.Litecoin -> LitecoinAdapter(wallet, derivation, syncMode, appConfigProvider.testMode)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, syncMode, appConfigProvider.testMode)
            is CoinType.Dash -> DashAdapter(wallet, syncMode, appConfigProvider.testMode)
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
