package io.horizontalsystems.walletkit.core.factories

import android.content.Context
import android.util.Log
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.BitcoinAdapter
import io.horizontalsystems.walletkit.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.walletkit.core.adapters.DashAdapter
import io.horizontalsystems.walletkit.core.adapters.ECashAdapter
import io.horizontalsystems.walletkit.core.adapters.Eip20Adapter
import io.horizontalsystems.walletkit.core.adapters.EvmAdapter
import io.horizontalsystems.walletkit.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.LitecoinAdapter
import io.horizontalsystems.walletkit.core.adapters.ThorchainAdapter
import io.horizontalsystems.walletkit.core.adapters.ThorchainTransactionConverter
import io.horizontalsystems.walletkit.core.adapters.ThorchainTransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.Trc20Adapter
import io.horizontalsystems.walletkit.core.adapters.TronAdapter
import io.horizontalsystems.walletkit.core.adapters.TronTransactionConverter
import io.horizontalsystems.walletkit.core.adapters.TronTransactionsAdapter
import io.horizontalsystems.walletkit.core.managers.BtcBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmLabelManager
import io.horizontalsystems.walletkit.core.managers.EvmSyncSourceManager
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.managers.ThorchainKitManager
import io.horizontalsystems.walletkit.core.managers.TronKitManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AdapterFactory(
    private val context: Context,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val tronKitManager: TronKitManager,
    private val thorchainKitManager: ThorchainKitManager,
    private val mayachainKitManager: ThorchainKitManager,
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

    private fun getTrc20Adapter(wallet: Wallet, address: String): Trc20Adapter? {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(wallet.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: return null

        return Trc20Adapter(tronKitWrapper, address, wallet, coinManager, baseToken, evmLabelManager)
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

            BlockchainType.Tron -> {
                TronAdapter(tronKitManager.getTronKitWrapper(wallet.account))
            }
            BlockchainType.Thorchain,
            BlockchainType.Mayachain -> {
                ThorchainAdapter(thorchainFamilyKitManager(wallet.token.blockchainType).getThorchainKitWrapper(wallet.account), wallet)
            }
            else -> registryAdapter(wallet)
        }
        is TokenType.Eip20 -> {
            if (wallet.token.blockchainType == BlockchainType.Tron) {
                getTrc20Adapter(wallet, tokenType.address)
            } else {
                getEip20Adapter(wallet, tokenType.address)
            }
        }
        is TokenType.Spl -> registryAdapter(wallet)
        is TokenType.Jetton -> registryAdapter(wallet)
        is TokenType.Asset -> registryAdapter(wallet)
        is TokenType.ThorchainAsset -> when (wallet.token.blockchainType) {
            BlockchainType.Thorchain,
            BlockchainType.Mayachain -> ThorchainAdapter(thorchainFamilyKitManager(wallet.token.blockchainType).getThorchainKitWrapper(wallet.account), wallet)
            else -> null
        }
        is TokenType.ZanoAsset -> registryAdapter(wallet)
        is TokenType.Unsupported -> null
    }

    private fun registryAdapter(wallet: Wallet): IAdapter? =
        ChainRegistry[wallet.token.blockchainType]?.createAdapter(
            wallet,
            restoreSettingsManager.settings(wallet.account, wallet.token.blockchainType),
        )

    fun evmTransactionsAdapter(source: TransactionSource, blockchainType: BlockchainType): ITransactionsAdapter? {
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(source.account, blockchainType)
        val baseCoin = evmBlockchainManager.getBaseToken(blockchainType) ?: return null
        val syncSource = evmSyncSourceManager.getSyncSource(blockchainType)

        return EvmTransactionsAdapter(evmKitWrapper, baseCoin, coinManager, source, syncSource.transactionSource, evmLabelManager)
    }

    fun tronTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(source.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: return null
        val tronTransactionConverter = TronTransactionConverter(coinManager, tronKitWrapper, source, baseToken, evmLabelManager)

        return TronTransactionsAdapter(tronKitWrapper, tronTransactionConverter)
    }

    // THORChain (RUNE) and Maya (CACAO) share the ThorchainKit; pick the right per-chain manager.
    private fun thorchainFamilyKitManager(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Mayachain -> mayachainKitManager
        else -> thorchainKitManager
    }

    fun thorchainTransactionsAdapter(source: TransactionSource, blockchainType: BlockchainType): ITransactionsAdapter? {
        val tokenQuery = TokenQuery(blockchainType, TokenType.Native)
        val baseToken = coinManager.getToken(tokenQuery) ?: return null

        val thorchainKitWrapper = thorchainFamilyKitManager(blockchainType).getThorchainKitWrapper(source.account)

        val transactionConverter = ThorchainTransactionConverter(
            coinManager,
            source,
            thorchainKitWrapper.thorchainKit.receiveAddress,
            baseToken,
            blockchainType,
            thorchainKitWrapper.thorchainKit.network,
        )

        return ThorchainTransactionsAdapter(thorchainKitWrapper, transactionConverter)
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
            BlockchainType.Tron -> {
                tronKitManager.unlink(wallet.account)
            }
            BlockchainType.Thorchain -> {
                thorchainKitManager.unlink(wallet.account)
            }
            BlockchainType.Mayachain -> {
                mayachainKitManager.unlink(wallet.account)
            }
            else -> {
                ChainRegistry[blockchainType]?.unlink(wallet.account)
            }
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
                ChainRegistry[blockchainType]?.unlink(transactionSource.account)
            }
            BlockchainType.Tron -> {
                tronKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Ton -> {
                ChainRegistry[blockchainType]?.unlink(transactionSource.account)
            }
            BlockchainType.Stellar -> {
                ChainRegistry[blockchainType]?.unlink(transactionSource.account)
            }
            BlockchainType.Thorchain -> {
                thorchainKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Mayachain -> {
                mayachainKitManager.unlink(transactionSource.account)
            }
            else -> Unit
        }
    }
}
