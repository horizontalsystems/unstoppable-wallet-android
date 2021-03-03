package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.IEthereumRpcModeSettingsManager
import io.horizontalsystems.bankwallet.core.IInitialSyncModeSettingsManager
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.BinanceKitManager
import io.horizontalsystems.bankwallet.core.managers.BinanceSmartChainKitManager
import io.horizontalsystems.bankwallet.core.managers.BitcoinCashCoinTypeManager
import io.horizontalsystems.bankwallet.core.managers.EthereumKitManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.BackgroundManager

class AdapterFactory(
        private val context: Context,
        private val testMode: Boolean,
        private val ethereumKitManager: EthereumKitManager,
        private val binanceSmartChainKitManager: BinanceSmartChainKitManager,
        private val binanceKitManager: BinanceKitManager,
        private val backgroundManager: BackgroundManager) {

    var initialSyncModeSettingsManager: IInitialSyncModeSettingsManager? = null
    var derivationSettingsManager: IDerivationSettingsManager? = null
    var ethereumRpcModeSettingsManager: IEthereumRpcModeSettingsManager? = null
    var bitcoinCashCoinTypeManager: BitcoinCashCoinTypeManager? = null

    fun adapter(wallet: Wallet): IAdapter? {
        val derivation = derivationSettingsManager?.setting(wallet.coin.type)?.derivation
        val syncMode = initialSyncModeSettingsManager?.setting(wallet.coin.type, wallet.account.origin)?.syncMode

        return when (val coinType = wallet.coin.type) {
            is CoinType.Zcash -> ZcashAdapter(context, wallet, testMode)
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, derivation, syncMode, testMode, backgroundManager)
            is CoinType.Litecoin -> LitecoinAdapter(wallet, derivation, syncMode, testMode, backgroundManager)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, syncMode, bitcoinCashCoinTypeManager?.bitcoinCashCoinType, testMode, backgroundManager)
            is CoinType.Dash -> DashAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Bep2 -> BinanceAdapter(binanceKitManager.binanceKit(wallet), coinType.symbol)
            is CoinType.Ethereum -> EvmAdapter(ethereumKitManager.evmKit(wallet.account))
            is CoinType.Erc20 -> Eip20Adapter(context, ethereumKitManager.evmKit(wallet.account), wallet.coin.decimal, coinType.address)
            is CoinType.BinanceSmartChain -> EvmAdapter(binanceSmartChainKitManager.evmKit(wallet.account))
            is CoinType.Bep20 -> Eip20Adapter(context, binanceSmartChainKitManager.evmKit(wallet.account), wallet.coin.decimal, coinType.address)
            is CoinType.Unsupported -> null
        }
    }

    fun unlinkAdapter(coinType: CoinType) {
        when (coinType) {
            CoinType.Ethereum, is CoinType.Erc20 -> {
                ethereumKitManager.unlink()
            }
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> {
                binanceSmartChainKitManager.unlink()
            }
            is CoinType.Bep2 -> {
                binanceKitManager.unlink()
            }
        }
    }
}
