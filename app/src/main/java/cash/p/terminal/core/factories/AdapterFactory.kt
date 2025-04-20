package cash.p.terminal.core.factories

import android.content.Context
import android.util.Log
import cash.p.terminal.core.App
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.adapters.BinanceAdapter
import cash.p.terminal.core.adapters.BitcoinAdapter
import cash.p.terminal.core.adapters.BitcoinCashAdapter
import cash.p.terminal.core.adapters.CosantaAdapter
import cash.p.terminal.core.adapters.DashAdapter
import cash.p.terminal.core.adapters.DogecoinAdapter
import cash.p.terminal.core.adapters.ECashAdapter
import cash.p.terminal.core.adapters.Eip20Adapter
import cash.p.terminal.core.adapters.EvmAdapter
import cash.p.terminal.core.adapters.EvmTransactionsAdapter
import cash.p.terminal.core.adapters.JettonAdapter
import cash.p.terminal.core.adapters.LitecoinAdapter
import cash.p.terminal.core.adapters.SolanaAdapter
import cash.p.terminal.core.adapters.SolanaTransactionConverter
import cash.p.terminal.core.adapters.SolanaTransactionsAdapter
import cash.p.terminal.core.adapters.SplAdapter
import cash.p.terminal.core.adapters.TonAdapter
import cash.p.terminal.core.adapters.TonTransactionConverter
import cash.p.terminal.core.adapters.TonTransactionsAdapter
import cash.p.terminal.core.adapters.Trc20Adapter
import cash.p.terminal.core.adapters.TronAdapter
import cash.p.terminal.core.adapters.TronTransactionConverter
import cash.p.terminal.core.adapters.TronTransactionsAdapter
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.managers.BinanceKitManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.StackingManager
import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.network.pirate.domain.repository.MasterNodesRepository
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.tonkit.Address
import org.koin.java.KoinJavaComponent.inject

class AdapterFactory(
    private val context: Context,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val binanceKitManager: BinanceKitManager,
    private val solanaKitManager: SolanaKitManager,
    private val tronKitManager: TronKitManager,
    private val tonKitManager: TonKitManager,
    private val backgroundManager: BackgroundManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val coinManager: ICoinManager,
    private val evmLabelManager: EvmLabelManager,
    private val localStorage: ILocalStorage,
    private val masterNodesRepository: MasterNodesRepository
) {

    private fun getEvmAdapter(wallet: Wallet): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null

        val evmTransactionRepository: EvmTransactionRepository by inject(
            EvmTransactionRepository::class.java
        )
        evmTransactionRepository.setup(
            account = wallet.account,
            blockchainType = blockchainType
        )

        return EvmAdapter(evmTransactionRepository, coinManager)
    }

    private fun getEip20Adapter(wallet: Wallet, address: String): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null
        val baseToken = evmBlockchainManager.getBaseToken(blockchainType) ?: return null
        val stackingManager = getKoinInstance<StackingManager>()

        val evmTransactionRepository: EvmTransactionRepository by inject(
            EvmTransactionRepository::class.java
        )
        evmTransactionRepository.setup(
            account = wallet.account,
            blockchainType = blockchainType
        )

        return Eip20Adapter(
            context = context,
            evmTransactionRepository = evmTransactionRepository,
            contractAddress = address,
            baseToken = baseToken,
            coinManager = coinManager,
            wallet = wallet,
            evmLabelManager = evmLabelManager,
            stackingManager = stackingManager
        )
    }

    private fun getSplAdapter(wallet: Wallet, address: String): IAdapter? {
        val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(wallet.account)

        return SplAdapter(solanaKitWrapper, wallet, address)
    }

    private fun getTrc20Adapter(wallet: Wallet, address: String): IAdapter {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(wallet.account)

        return Trc20Adapter(tronKitWrapper, address, wallet)
    }

    private fun getJettonAdapter(wallet: Wallet, address: String): IAdapter {
        val tonKitWrapper = tonKitManager.getTonKitWrapper(wallet.account)

        return JettonAdapter(tonKitWrapper, address, wallet)
    }

    fun getAdapterOrNull(wallet: Wallet) = try {
        getAdapter(wallet)
    } catch (e: Throwable) {
        Log.e("AAA", "get adapter error", e)
        null
    }

    private fun getAdapter(wallet: Wallet) =
        when (val tokenType = wallet.token.type) {
            is TokenType.Derived -> {
                when (wallet.token.blockchainType) {
                    BlockchainType.Bitcoin -> {
                        val syncMode = btcBlockchainManager.syncMode(
                            BlockchainType.Bitcoin,
                            wallet.account.origin
                        )
                        BitcoinAdapter(
                            wallet = wallet,
                            syncMode = syncMode,
                            backgroundManager = backgroundManager,
                            derivation = tokenType.derivation
                        )
                    }

                    BlockchainType.Litecoin -> {
                        val syncMode = btcBlockchainManager.syncMode(
                            BlockchainType.Litecoin,
                            wallet.account.origin
                        )
                        LitecoinAdapter(
                            wallet = wallet,
                            syncMode = syncMode,
                            backgroundManager = backgroundManager,
                            derivation = tokenType.derivation
                        )
                    }

                    else -> null
                }
            }

            is TokenType.AddressTyped -> {
                if (wallet.token.blockchainType == BlockchainType.BitcoinCash) {
                    val syncMode = btcBlockchainManager.syncMode(
                        BlockchainType.BitcoinCash,
                        wallet.account.origin
                    )
                    BitcoinCashAdapter(
                        wallet = wallet,
                        syncMode = syncMode,
                        backgroundManager = backgroundManager,
                        addressType = tokenType.type
                    )
                } else null
            }

            is TokenType.AddressSpecTyped -> {
                when (wallet.token.blockchainType) {
                    BlockchainType.Zcash -> {
                        ZcashAdapter(
                            context = context,
                            wallet = wallet,
                            restoreSettings = restoreSettingsManager.settings(
                                wallet.account,
                                wallet.token.blockchainType
                            ),
                            addressSpecTyped = tokenType.type,
                            localStorage = localStorage,
                            backgroundManager = backgroundManager
                        )
                    }

                    else -> null
                }
            }

            TokenType.Native -> when (wallet.token.blockchainType) {
                BlockchainType.ECash -> {
                    val syncMode =
                        btcBlockchainManager.syncMode(BlockchainType.ECash, wallet.account.origin)
                    ECashAdapter(wallet, syncMode, backgroundManager)
                }

                BlockchainType.Dash -> {
                    val syncMode =
                        btcBlockchainManager.syncMode(BlockchainType.Dash, wallet.account.origin)
                    DashAdapter(
                        wallet = wallet,
                        syncMode = syncMode,
                        backgroundManager = backgroundManager,
                        customPeers = localStorage.customDashPeers,
                        masterNodesRepository = masterNodesRepository
                    )
                }

                BlockchainType.Cosanta -> {
                    val syncMode =
                        btcBlockchainManager.syncMode(BlockchainType.Cosanta, wallet.account.origin)
                    CosantaAdapter(
                        wallet = wallet,
                        syncMode = syncMode,
                        backgroundManager = backgroundManager
                    )
                }

                BlockchainType.Dogecoin -> {
                    val syncMode = btcBlockchainManager.syncMode(
                        BlockchainType.Dogecoin,
                        wallet.account.origin
                    )
                    DogecoinAdapter(
                        wallet = wallet,
                        syncMode = syncMode,
                        backgroundManager = backgroundManager
                    )
                }

                BlockchainType.Zcash -> {
                    ZcashAdapter(
                        context = context,
                        wallet = wallet,
                        restoreSettings = restoreSettingsManager.settings(
                            wallet.account,
                            wallet.token.blockchainType
                        ),
                        addressSpecTyped = null,
                        localStorage = localStorage,
                        backgroundManager = backgroundManager
                    )
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

                BlockchainType.BinanceChain -> {
                    getBinanceAdapter(wallet, "BNB")
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

                else -> null
            }

            is TokenType.Eip20 -> {
                if (wallet.token.blockchainType == BlockchainType.Tron) {
                    getTrc20Adapter(wallet, tokenType.address)
                } else {
                    getEip20Adapter(wallet, tokenType.address)
                }
            }

            is TokenType.Bep2 -> getBinanceAdapter(wallet, tokenType.symbol)
            is TokenType.Spl -> getSplAdapter(wallet, tokenType.address)
            is TokenType.Jetton -> getJettonAdapter(wallet, tokenType.address)
            is TokenType.Unsupported -> null
        }

    private fun getBinanceAdapter(
        wallet: Wallet,
        symbol: String
    ): BinanceAdapter? {
        val query = TokenQuery(BlockchainType.BinanceChain, TokenType.Native)
        return coinManager.getToken(query)?.let { feeToken ->
            BinanceAdapter(binanceKitManager.binanceKit(wallet), symbol, feeToken, wallet)
        }
    }

    fun evmTransactionsAdapter(
        source: TransactionSource,
        blockchainType: BlockchainType
    ): ITransactionsAdapter? {
        val evmTransactionRepository: EvmTransactionRepository by inject(
            EvmTransactionRepository::class.java
        )
        evmTransactionRepository.setup(
            account = source.account,
            blockchainType = blockchainType
        )
        val baseCoin = evmBlockchainManager.getBaseToken(blockchainType) ?: return null
        val syncSource = evmSyncSourceManager.getSyncSource(blockchainType)

        return EvmTransactionsAdapter(
            evmTransactionRepository = evmTransactionRepository,
            baseToken = baseCoin,
            coinManager = coinManager,
            source = source,
            evmTransactionSource = syncSource.transactionSource,
            evmLabelManager = evmLabelManager
        )
    }

    fun solanaTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(source.account)
        val baseToken =
            coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native)) ?: return null
        val solanaTransactionConverter =
            SolanaTransactionConverter(
                coinManager = coinManager,
                source = source,
                baseToken = baseToken,
                spamManager = App.spamManager,
                solanaKitWrapper = solanaKitWrapper
            )

        return SolanaTransactionsAdapter(solanaKitWrapper, solanaTransactionConverter)
    }

    fun tronTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(source.account)
        val baseToken =
            coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: return null
        val tronTransactionConverter = TronTransactionConverter(
            coinManager,
            tronKitWrapper,
            source,
            baseToken,
            evmLabelManager
        )

        return TronTransactionsAdapter(tronKitWrapper, tronTransactionConverter)
    }

    fun tonTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tonKitWrapper = tonKitManager.getTonKitWrapper(source.account)
        val address = tonKitWrapper.tonKit.receiveAddress

        val tonTransactionConverter = tonTransactionConverter(address, source) ?: return null

        return TonTransactionsAdapter(tonKitWrapper, tonTransactionConverter)
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

            BlockchainType.BinanceChain -> {
                binanceKitManager.unlink(wallet.account)
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

            else -> Unit
        }
    }
}
