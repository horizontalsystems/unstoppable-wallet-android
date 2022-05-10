package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.providers.TokenBalanceProvider
import io.horizontalsystems.bankwallet.core.storage.EvmAccountStateDao
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmAccountState
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.oneinchkit.decorations.OneInchDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.max

class EvmAccountManager(
    private val blockchain: EvmBlockchain,
    private val accountManager: IAccountManager,
    private val evmKitManager: EvmKitManager,
    private val provider: TokenBalanceProvider,
    private val evmAccountStateDao: EvmAccountStateDao,
    private val walletActivator: WalletActivator
) {
    private val logger = AppLogger("evm-account-manager")
    private var disposables = CompositeDisposable()
    private var transactionsDisposable: Disposable? = null

    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(singleDispatcher)

    init {
        evmKitManager.kitStartedObservable
            .subscribeIO { started ->
                coroutineScope.launch {
                    try {
                        if (started) {
                            start()
                        } else {
                            stop()
                        }
                    } catch (exception: Exception) {
                        logger.warning("error", exception)
                    }
                }
            }
            .let { disposables.add(it) }
    }

    suspend fun start() {
        initialSync()

        subscribeToTransactions()
    }

    private fun stop() {
        transactionsDisposable?.dispose()
    }

    private suspend fun initialSync() {
        val account = accountManager.activeAccount ?: return
        val evmKitWrapper = evmKitManager.evmKitWrapper ?: return

        val chainId = evmKitManager.chain.id
        val syncState = evmAccountStateDao.get(account.id, chainId)

        if (syncState == null) {
            // blockchain is enabled after restore

            evmAccountStateDao.insert(EvmAccountState(account.id, chainId, provider.blockNumber(blockchain)))
        } else if (syncState.transactionsSyncedBlockNumber == 0L) {
            // blockchain is enabled during restore or 'watch address'

            val addressInfo = provider.addresses(evmKitWrapper.evmKit.receiveAddress.hex, blockchain)
            val coinTypes = addressInfo.addresses.map { blockchain.getEvm20CoinType(it) }
            walletActivator.activateWallets(account, coinTypes)

            evmAccountStateDao.insert(EvmAccountState(account.id, chainId, addressInfo.blockNumber))
        }
    }

    private fun subscribeToTransactions() {
        val evmKitWrapper = evmKitManager.evmKitWrapper ?: return
        val account = accountManager.activeAccount ?: return

        evmKitWrapper.evmKit.allTransactionsFlowable
            .subscribeIO { fullTransactions ->
                handle(fullTransactions, account, evmKitWrapper)
            }
            .let { disposables.add(it) }
    }

    private fun handle(fullTransactions: List<FullTransaction>, account: Account, evmKitWrapper: EvmKitWrapper) {
        val address = evmKitWrapper.evmKit.receiveAddress
        val lastBlockNumber =
            evmAccountStateDao.get(account.id, evmKitManager.chain.id)?.transactionsSyncedBlockNumber ?: 0

        val coinTypes = mutableListOf<CoinType>()
        var maxBlockNumber = 0L

        for (fullTransaction in fullTransactions) {
            val blockNumber = fullTransaction.transaction.blockNumber ?: 0

            if (blockNumber <= lastBlockNumber) continue

            maxBlockNumber = max(maxBlockNumber, blockNumber)

            val decoration = fullTransaction.decoration

            when (decoration) {
                is IncomingDecoration -> {
                    coinTypes.add(blockchain.baseCoinType)
                }

                is SwapDecoration -> {
                    val tokenOut = decoration.tokenOut
                    if (tokenOut is SwapDecoration.Token.Eip20Coin) {
                        coinTypes.add(blockchain.getEvm20CoinType(tokenOut.address.hex))
                    }
                }

                is OneInchSwapDecoration -> {
                    val tokenOut = decoration.tokenOut
                    if (tokenOut is OneInchDecoration.Token.Eip20Coin) {
                        coinTypes.add(blockchain.getEvm20CoinType(tokenOut.address.hex))
                    }
                }

                is OneInchUnoswapDecoration -> {
                    val tokenOut = decoration.tokenOut
                    if (tokenOut is OneInchDecoration.Token.Eip20Coin) {
                        coinTypes.add(blockchain.getEvm20CoinType(tokenOut.address.hex))
                    }
                }

                is UnknownTransactionDecoration -> {
                    if (decoration.internalTransactions.any { it.to == address }) {
                        coinTypes.add(blockchain.baseCoinType)
                    }

                    for (eventInstance in decoration.eventInstances) {
                        if (eventInstance !is TransferEventInstance) continue

                        if (eventInstance.to == address) {
                            coinTypes.add(blockchain.getEvm20CoinType(eventInstance.contractAddress.hex))
                        }
                    }
                }
            }
        }

        if (maxBlockNumber > 0) {
            evmAccountStateDao.insert(EvmAccountState(account.id, evmKitManager.chain.id, maxBlockNumber))
        }

        walletActivator.activateWallets(account, coinTypes)
    }


    fun markAutoEnable(account: Account) {
        evmAccountStateDao.insert(EvmAccountState(account.id, evmKitManager.chain.id, 0))
    }

}
