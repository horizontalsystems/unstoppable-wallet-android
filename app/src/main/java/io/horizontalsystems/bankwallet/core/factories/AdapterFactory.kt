package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.entities.BtcBlockchain
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.xxxkit.models.BlockchainType
import io.horizontalsystems.xxxkit.models.TokenQuery
import io.horizontalsystems.xxxkit.models.TokenType

class AdapterFactory(
    private val context: Context,
    private val testMode: Boolean,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val binanceKitManager: BinanceKitManager,
    private val backgroundManager: BackgroundManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val coinManager: ICoinManager,
    private val evmLabelManager: EvmLabelManager,
) {

    private fun getEvmAdapter(wallet: Wallet): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(
            wallet.account,
            blockchainType
        )

        return EvmAdapter(evmKitWrapper, coinManager)
    }

    private fun getEip20Adapter(wallet: Wallet, address: String): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(wallet.account, blockchainType)
        val baseToken = evmBlockchainManager.getBaseToken(blockchainType) ?: return null

        return Eip20Adapter(context, evmKitWrapper, address, baseToken, coinManager, wallet, evmLabelManager)
    }

    fun getAdapter(wallet: Wallet) = when (val tokenType = wallet.token.type) {
        TokenType.Native -> when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin -> {
                val syncMode = btcBlockchainManager.syncMode(BtcBlockchain.Bitcoin, wallet.account.origin)
                BitcoinAdapter(wallet, syncMode, testMode, backgroundManager)
            }
            BlockchainType.BitcoinCash -> {
                val syncMode = btcBlockchainManager.syncMode(BtcBlockchain.BitcoinCash, wallet.account.origin)
                BitcoinCashAdapter(wallet, syncMode, testMode, backgroundManager)
            }
            BlockchainType.Litecoin -> {
                val syncMode = btcBlockchainManager.syncMode(BtcBlockchain.Litecoin, wallet.account.origin)
                LitecoinAdapter(wallet, syncMode, testMode, backgroundManager)
            }
            BlockchainType.Dash -> {
                val syncMode = btcBlockchainManager.syncMode(BtcBlockchain.Dash, wallet.account.origin)
                DashAdapter(wallet, syncMode, testMode, backgroundManager)
            }
            BlockchainType.Zcash -> {
                ZcashAdapter(context, wallet, restoreSettingsManager.settings(wallet.account, wallet.coinType), testMode)
            }
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> getEvmAdapter(wallet)
            BlockchainType.BinanceChain -> getBinanceAdapter(wallet, "BNB")
            is BlockchainType.Unsupported -> null
        }
        is TokenType.Eip20 -> getEip20Adapter(wallet, tokenType.address)
        is TokenType.Bep2 -> getBinanceAdapter(wallet, tokenType.symbol)
        is TokenType.Unsupported -> null
    }

    private fun getBinanceAdapter(
        wallet: Wallet,
        symbol: String
    ): BinanceAdapter? {
        val query = TokenQuery(BlockchainType.BinanceChain, TokenType.Native)
        return coinManager.getToken(query)?.let { feeToken ->
            BinanceAdapter(
                binanceKitManager.binanceKit(wallet),
                symbol, feeToken, wallet, testMode
            )
        }
    }

    fun evmTransactionsAdapter(source: TransactionSource, blockchainType: BlockchainType): ITransactionsAdapter? {
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(source.account, blockchainType)
        val baseCoin = evmBlockchainManager.getBaseToken(blockchainType) ?: return null
        val syncSource = evmSyncSourceManager.getSyncSource(blockchainType)

        return EvmTransactionsAdapter(evmKitWrapper, baseCoin, coinManager, source, syncSource.transactionSource, evmLabelManager)
    }

    fun unlinkAdapter(wallet: Wallet) {
        when (val blockchain = wallet.transactionSource.blockchain) {
            is TransactionSource.Blockchain.Evm -> {
                val blockchainType = blockchain.blockchainType
                val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)
                evmKitManager.unlink(wallet.account)
            }
            is TransactionSource.Blockchain.Bep2 -> {
                binanceKitManager.unlink(wallet.account)
            }
            else -> Unit
        }
    }

    fun unlinkAdapter(transactionSource: TransactionSource) {
        when (val blockchain = transactionSource.blockchain) {
            is TransactionSource.Blockchain.Evm -> {
                val blockchainType = blockchain.blockchainType
                val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)
                evmKitManager.unlink(transactionSource.account)
            }
            else -> Unit
        }
    }
}
