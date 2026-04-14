package com.quantum.wallet.bankwallet.core.factories

import android.content.Context
import android.util.Log
import com.quantum.wallet.bankwallet.core.BackgroundManager
import com.quantum.wallet.bankwallet.core.IAdapter
import com.quantum.wallet.bankwallet.core.ICoinManager
import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.core.ITransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.BitcoinAdapter
import com.quantum.wallet.bankwallet.core.adapters.BitcoinCashAdapter
import com.quantum.wallet.bankwallet.core.adapters.DashAdapter
import com.quantum.wallet.bankwallet.core.adapters.ECashAdapter
import com.quantum.wallet.bankwallet.core.adapters.Eip20Adapter
import com.quantum.wallet.bankwallet.core.adapters.EvmAdapter
import com.quantum.wallet.bankwallet.core.adapters.EvmTransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.JettonAdapter
import com.quantum.wallet.bankwallet.core.adapters.LitecoinAdapter
import com.quantum.wallet.bankwallet.core.adapters.MoneroAdapter
import com.quantum.wallet.bankwallet.core.adapters.SolanaAdapter
import com.quantum.wallet.bankwallet.core.adapters.SolanaTransactionConverter
import com.quantum.wallet.bankwallet.core.adapters.SolanaTransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.SplAdapter
import com.quantum.wallet.bankwallet.core.adapters.StellarAdapter
import com.quantum.wallet.bankwallet.core.adapters.StellarAssetAdapter
import com.quantum.wallet.bankwallet.core.adapters.StellarTransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.TonAdapter
import com.quantum.wallet.bankwallet.core.adapters.TonTransactionConverter
import com.quantum.wallet.bankwallet.core.adapters.TonTransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.Trc20Adapter
import com.quantum.wallet.bankwallet.core.adapters.TronAdapter
import com.quantum.wallet.bankwallet.core.adapters.TronTransactionConverter
import com.quantum.wallet.bankwallet.core.adapters.TronTransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.zcash.ZcashAdapter
import com.quantum.wallet.bankwallet.core.managers.BtcBlockchainManager
import com.quantum.wallet.bankwallet.core.managers.EvmBlockchainManager
import com.quantum.wallet.bankwallet.core.managers.EvmLabelManager
import com.quantum.wallet.bankwallet.core.managers.EvmSyncSourceManager
import com.quantum.wallet.bankwallet.core.managers.MoneroNodeManager
import com.quantum.wallet.bankwallet.core.managers.RestoreSettingsManager
import com.quantum.wallet.bankwallet.core.managers.SolanaKitManager
import com.quantum.wallet.bankwallet.core.managers.StellarKitManager
import com.quantum.wallet.bankwallet.core.managers.TonKitManager
import com.quantum.wallet.bankwallet.core.managers.TronKitManager
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.Address

class AdapterFactory(
    private val context: Context,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val solanaKitManager: SolanaKitManager,
    private val tronKitManager: TronKitManager,
    private val tonKitManager: TonKitManager,
    private val stellarKitManager: StellarKitManager,
    private val moneroNodeManager: MoneroNodeManager,
    private val backgroundManager: BackgroundManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val coinManager: ICoinManager,
    private val evmLabelManager: EvmLabelManager,
    private val localStorage: ILocalStorage,
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

    private fun getSplAdapter(wallet: Wallet, address: String): IAdapter? {
        val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(wallet.account)

        return SplAdapter(solanaKitWrapper, wallet, address)
    }

    private fun getTrc20Adapter(wallet: Wallet, address: String): Trc20Adapter? {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(wallet.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: return null

        return Trc20Adapter(tronKitWrapper, address, wallet, coinManager, baseToken, evmLabelManager)
    }

    private fun getJettonAdapter(wallet: Wallet, address: String): IAdapter {
        val tonKitWrapper = tonKitManager.getTonKitWrapper(wallet.account)

        return JettonAdapter(tonKitWrapper, address, wallet)
    }

    private fun getStellarAssetAdapter(wallet: Wallet, code: String, issuer: String): IAdapter {
        val stellarKitWrapper = stellarKitManager.getStellarKitWrapper(wallet.account)

        return StellarAssetAdapter(stellarKitWrapper, code, issuer)
    }

    fun getAdapterOrNull(wallet: Wallet) = try {
        getAdapter(wallet)
    } catch (e: Throwable) {
        Log.e("AAA", "get adapter error", e)
        null
    }

    private fun getAdapter(wallet: Wallet) = when (val tokenType = wallet.token.type) {
        is TokenType.Derived -> {
            when (wallet.token.blockchainType) {
                BlockchainType.Bitcoin -> {
                    val syncMode = btcBlockchainManager.syncMode(BlockchainType.Bitcoin, wallet.account.origin)
                    BitcoinAdapter(wallet, syncMode, backgroundManager, tokenType.derivation)
                }
                BlockchainType.Litecoin -> {
                    val syncMode = btcBlockchainManager.syncMode(BlockchainType.Litecoin, wallet.account.origin)
                    LitecoinAdapter(wallet, syncMode, backgroundManager, tokenType.derivation)
                }
                else -> null
            }
        }
        is TokenType.AddressTyped -> {
            if (wallet.token.blockchainType == BlockchainType.BitcoinCash) {
                val syncMode = btcBlockchainManager.syncMode(BlockchainType.BitcoinCash, wallet.account.origin)
                BitcoinCashAdapter(wallet, syncMode, backgroundManager, tokenType.type)
            }
            else null
        }
        TokenType.Native -> when (wallet.token.blockchainType) {
            BlockchainType.ECash -> {
                val syncMode = btcBlockchainManager.syncMode(BlockchainType.ECash, wallet.account.origin)
                ECashAdapter(wallet, syncMode, backgroundManager)
            }
            BlockchainType.Dash -> {
                val syncMode = btcBlockchainManager.syncMode(BlockchainType.Dash, wallet.account.origin)
                DashAdapter(wallet, syncMode, backgroundManager)
            }
            BlockchainType.Zcash -> {
                ZcashAdapter(context, wallet, restoreSettingsManager.settings(wallet.account, wallet.token.blockchainType), localStorage)
            }
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                getEvmAdapter(wallet)
            }

            BlockchainType.Solana -> {
                val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(wallet.account)
                SolanaAdapter(solanaKitWrapper)
            }
            BlockchainType.Tron -> {
                TronAdapter(tronKitManager.getTronKitWrapper(wallet.account))
            }
            BlockchainType.Ton -> {
                TonAdapter(tonKitManager.getTonKitWrapper(wallet.account))
            }
            BlockchainType.Stellar -> {
                StellarAdapter(stellarKitManager.getStellarKitWrapper(wallet.account))
            }
            BlockchainType.Monero -> {
                MoneroAdapter.create(
                    context = context,
                    wallet = wallet,
                    restoreSettings = restoreSettingsManager.settings(wallet.account, wallet.token.blockchainType),
                    node = moneroNodeManager.currentNode
                )
            }

            else -> null
        }
        is TokenType.Eip20 -> {
            if (wallet.token.blockchainType == BlockchainType.Tron) {
                getTrc20Adapter(wallet, tokenType.address)
            } else {
                getEip20Adapter(wallet, tokenType.address)
            }
        }
        is TokenType.Spl -> getSplAdapter(wallet, tokenType.address)
        is TokenType.Jetton -> getJettonAdapter(wallet, tokenType.address)
        is TokenType.Asset -> getStellarAssetAdapter(wallet, tokenType.code, tokenType.issuer)
        is TokenType.Unsupported -> null
    }

    fun evmTransactionsAdapter(source: TransactionSource, blockchainType: BlockchainType): ITransactionsAdapter? {
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(source.account, blockchainType)
        val baseCoin = evmBlockchainManager.getBaseToken(blockchainType) ?: return null
        val syncSource = evmSyncSourceManager.getSyncSource(blockchainType)

        return EvmTransactionsAdapter(evmKitWrapper, baseCoin, coinManager, source, syncSource.transactionSource, evmLabelManager)
    }

    fun solanaTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(source.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native)) ?: return null
        val solanaTransactionConverter = SolanaTransactionConverter(coinManager, source, baseToken, solanaKitWrapper)

        return SolanaTransactionsAdapter(solanaKitWrapper, solanaTransactionConverter)
    }

    fun tronTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(source.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: return null
        val tronTransactionConverter = TronTransactionConverter(coinManager, tronKitWrapper, source, baseToken, evmLabelManager)

        return TronTransactionsAdapter(tronKitWrapper, tronTransactionConverter)
    }

    fun tonTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tonKitWrapper = tonKitManager.getTonKitWrapper(source.account)
        val address = tonKitWrapper.tonKit.receiveAddress

        val tonTransactionConverter = tonTransactionConverter(address, source) ?: return null

        return TonTransactionsAdapter(tonKitWrapper, tonTransactionConverter)
    }

    fun stellarTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val stellarKitWrapper = stellarKitManager.getStellarKitWrapper(source.account)

        val tokenQuery = TokenQuery(BlockchainType.Stellar, TokenType.Native)
        val baseToken = coinManager.getToken(tokenQuery) ?: return null

        val transactionConverter = StellarTransactionConverter(
            source,
            stellarKitWrapper.stellarKit.receiveAddress,
            coinManager,
            baseToken
        )

        return StellarTransactionsAdapter(stellarKitWrapper, transactionConverter)
    }

    fun tonTransactionConverter(
        address: Address,
        source: TransactionSource,
    ): TonTransactionConverter? {
        val query = TokenQuery(BlockchainType.Ton, TokenType.Native)
        val baseToken = coinManager.getToken(query) ?: return null
        return TonTransactionConverter(
            address,
            coinManager,
            source,
            baseToken
        )
    }

    fun unlinkAdapter(wallet: Wallet) {
        when (val blockchainType = wallet.transactionSource.blockchain.type) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.ArbitrumOne -> {
                val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)
                evmKitManager.unlink(wallet.account)
            }
            BlockchainType.Solana -> {
                solanaKitManager.unlink(wallet.account)
            }
            BlockchainType.Tron -> {
                tronKitManager.unlink(wallet.account)
            }
            BlockchainType.Ton -> {
                tonKitManager.unlink(wallet.account)
            }
            BlockchainType.Stellar -> {
                stellarKitManager.unlink(wallet.account)
            }
            else -> Unit
        }
    }

    fun unlinkAdapter(transactionSource: TransactionSource) {
        when (val blockchainType = transactionSource.blockchain.type) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.ArbitrumOne -> {
                val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)
                evmKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Solana -> {
                solanaKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Tron -> {
                tronKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Ton -> {
                tonKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Stellar -> {
                stellarKitManager.unlink(transactionSource.account)
            }
            else -> Unit
        }
    }
}
