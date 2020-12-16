package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.BinanceKitManager
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.BackgroundManager

class AdapterFactory(
        private val context: Context,
        private val testMode: Boolean,
        private val ethereumKitManager: IEthereumKitManager,
        private val eosKitManager: IEosKitManager,
        private val binanceKitManager: BinanceKitManager,
        private val backgroundManager: BackgroundManager) {

    var initialSyncModeSettingsManager: IInitialSyncModeSettingsManager? = null
    var derivationSettingsManager: IDerivationSettingsManager? = null
    var ethereumRpcModeSettingsManager: IEthereumRpcModeSettingsManager? = null

    fun adapter(wallet: Wallet): IAdapter? {
        val derivation = derivationSettingsManager?.setting(wallet.coin.type)?.derivation
        val syncMode = initialSyncModeSettingsManager?.setting(wallet.coin.type, wallet.account.origin)?.syncMode
        val communicationMode = ethereumRpcModeSettingsManager?.rpcMode()?.communicationMode

        return when (val coinType = wallet.coin.type) {
            is CoinType.Zcash -> ZcashAdapter(context, wallet, testMode)
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, derivation, syncMode, testMode, backgroundManager)
            is CoinType.Litecoin -> LitecoinAdapter(wallet, derivation, syncMode, testMode, backgroundManager)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Dash -> DashAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Eos -> EosAdapter(coinType, eosKitManager.eosKit(wallet), wallet.coin.decimal)
            is CoinType.Binance -> BinanceAdapter(binanceKitManager.binanceKit(wallet), coinType.symbol)
            is CoinType.Ethereum -> EthereumAdapter(ethereumKitManager.ethereumKit(wallet, communicationMode))
            is CoinType.Erc20 -> Erc20Adapter(context, ethereumKitManager.ethereumKit(wallet, communicationMode), wallet.coin.decimal, coinType.address, coinType.fee, coinType.minimumRequiredBalance, coinType.minimumSendAmount)
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
