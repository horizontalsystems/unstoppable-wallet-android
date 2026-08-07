package io.horizontalsystems.walletkit.core.factories

import android.content.Context
import android.util.Log
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.evmTransactionSource
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.Eip20Adapter
import io.horizontalsystems.walletkit.core.adapters.EvmAdapter
import io.horizontalsystems.walletkit.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.Trc20Adapter
import io.horizontalsystems.walletkit.core.adapters.TronAdapter
import io.horizontalsystems.walletkit.core.adapters.TronTransactionConverter
import io.horizontalsystems.walletkit.core.adapters.TronTransactionsAdapter
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmLabelManager
import io.horizontalsystems.walletkit.core.managers.EvmSyncSourceManager
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.managers.TronKitManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AdapterFactory(
    private val context: Context,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val tronKitManager: TronKitManager,
    private val backgroundManager: BackgroundManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val coinManager: ICoinManager,
    private val evmLabelManager: EvmLabelManager,
    private val localStorage: ILocalStorage,
) {

    private fun getEvmAdapter(wallet: Wallet): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null
        val evmKitWrapper = EvmKitManagerRegistry.getEvmKitManager(blockchainType).getEvmKitWrapper(
            wallet.account,
            blockchainType
        )

        return EvmAdapter(evmKitWrapper, coinManager)
    }

    private fun getEip20Adapter(wallet: Wallet, address: String): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null
        val evmKitWrapper = EvmKitManagerRegistry.getEvmKitManager(blockchainType).getEvmKitWrapper(wallet.account, blockchainType)
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
        is TokenType.Derived -> registryAdapter(wallet)
        is TokenType.AddressTyped -> registryAdapter(wallet)
        TokenType.Native -> when (wallet.token.blockchainType) {
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
        is TokenType.ThorchainAsset -> registryAdapter(wallet)
        is TokenType.ZanoAsset -> registryAdapter(wallet)
        is TokenType.Unsupported -> null
    }

    private fun registryAdapter(wallet: Wallet): IAdapter? =
        ChainRegistry[wallet.token.blockchainType]?.createAdapter(
            wallet,
            restoreSettingsManager.settings(wallet.account, wallet.token.blockchainType),
        )

    fun evmTransactionsAdapter(source: TransactionSource, blockchainType: BlockchainType): ITransactionsAdapter? {
        val evmKitWrapper = EvmKitManagerRegistry.getEvmKitManager(blockchainType).getEvmKitWrapper(source.account, blockchainType)
        val baseCoin = evmBlockchainManager.getBaseToken(blockchainType) ?: return null
        val syncSource = evmSyncSourceManager.getSyncSource(blockchainType)

        return EvmTransactionsAdapter(evmKitWrapper, baseCoin, coinManager, source, evmTransactionSource(blockchainType, App.appConfigProvider), evmLabelManager)
    }

    fun tronTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(source.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: return null
        val tronTransactionConverter = TronTransactionConverter(coinManager, tronKitWrapper, source, baseToken, evmLabelManager)

        return TronTransactionsAdapter(tronKitWrapper, tronTransactionConverter)
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
                val evmKitManager = EvmKitManagerRegistry.getEvmKitManager(blockchainType)
                evmKitManager.unlink(wallet.account)
            }
            BlockchainType.Tron -> {
                tronKitManager.unlink(wallet.account)
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
                val evmKitManager = EvmKitManagerRegistry.getEvmKitManager(blockchainType)
                evmKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Tron -> {
                tronKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Thorchain -> {
                ChainRegistry[blockchainType]?.unlink(transactionSource.account)
            }
            BlockchainType.Mayachain -> {
                ChainRegistry[blockchainType]?.unlink(transactionSource.account)
            }
            else -> ChainRegistry[blockchainType]?.unlink(transactionSource.account)
        }
    }
}
