package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IInitialSyncModeSettingsManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.BinanceKitManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.CoinType

class AdapterFactory(
        private val context: Context,
        private val testMode: Boolean,
        private val ethereumKitManager: EvmKitManager,
        private val binanceSmartChainKitManager: EvmKitManager,
        private val binanceKitManager: BinanceKitManager,
        private val backgroundManager: BackgroundManager,
        private val restoreSettingsManager: RestoreSettingsManager,
        private val coinManager: ICoinManager) {

    var initialSyncModeSettingsManager: IInitialSyncModeSettingsManager? = null

    fun ethereumTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        return coinManager.getPlatformCoin(CoinType.Ethereum)?.let { baseCoin ->
            val evmKit = ethereumKitManager.evmKit(source.account)
            EvmTransactionsAdapter(evmKit, baseCoin, coinManager, source)
        }
    }

    fun bscTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        return coinManager.getPlatformCoin(CoinType.BinanceSmartChain)?.let { baseCoin ->
            val evmKit = binanceSmartChainKitManager.evmKit(source.account)
            EvmTransactionsAdapter(evmKit, baseCoin, coinManager, source)
        }
    }

    fun adapter(wallet: Wallet): IAdapter? {
        val syncMode = initialSyncModeSettingsManager?.setting(wallet.coinType, wallet.account.origin)?.syncMode ?: SyncMode.Fast

        return when (val coinType = wallet.coinType) {
            is CoinType.Zcash -> ZcashAdapter(context, wallet, restoreSettingsManager.settings(wallet.account, wallet.coinType), testMode)
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Litecoin -> LitecoinAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Dash -> DashAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Bep2 -> {
                coinManager.getPlatformCoin(CoinType.Bep2("BNB"))?.let { feePlatformCoin ->
                    BinanceAdapter(binanceKitManager.binanceKit(wallet), coinType.symbol, feePlatformCoin, wallet, testMode)
                }
            }
            is CoinType.Ethereum -> EvmAdapter(ethereumKitManager.evmKit(wallet.account), coinManager)
            is CoinType.Erc20 -> {
                coinManager.getPlatformCoin(CoinType.Ethereum)?.let { baseCoin ->
                    Eip20Adapter(context, ethereumKitManager.evmKit(wallet.account), coinType.address, baseCoin, coinManager, wallet)
                }
            }
            is CoinType.BinanceSmartChain -> EvmAdapter(binanceSmartChainKitManager.evmKit(wallet.account), coinManager)
            is CoinType.Bep20 -> {
                coinManager.getPlatformCoin(CoinType.BinanceSmartChain)?.let { baseCoin ->
                    Eip20Adapter(context, binanceSmartChainKitManager.evmKit(wallet.account), coinType.address, baseCoin, coinManager, wallet)
                }
            }
            is CoinType.ArbitrumOne,
            is CoinType.Avalanche,
            is CoinType.Fantom,
            is CoinType.HarmonyShard0,
            is CoinType.HuobiToken,
            is CoinType.Iotex,
            is CoinType.Moonriver,
            is CoinType.OkexChain,
            is CoinType.PolygonPos,
            is CoinType.Solana,
            is CoinType.Sora,
            is CoinType.Tomochain,
            is CoinType.Xdai,
            is CoinType.Unsupported -> null
        }
    }

    fun unlinkAdapter(wallet: Wallet) {
        when (wallet.coinType) {
            CoinType.Ethereum, is CoinType.Erc20 -> {
                ethereumKitManager.unlink(wallet.account)
            }
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> {
                binanceSmartChainKitManager.unlink(wallet.account)
            }
            is CoinType.Bep2 -> {
                binanceKitManager.unlink()
            }
            else -> Unit
        }
    }

    fun unlinkAdapter(transactionSource: TransactionSource) {
        when (transactionSource.blockchain) {
            TransactionSource.Blockchain.Ethereum -> {
                ethereumKitManager.unlink(transactionSource.account)
            }
            TransactionSource.Blockchain.BinanceSmartChain -> {
                binanceSmartChainKitManager.unlink(transactionSource.account)
            }
        }
    }
}
