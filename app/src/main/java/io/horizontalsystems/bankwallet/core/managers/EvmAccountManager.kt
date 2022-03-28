package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.providers.TokenBalanceProvider
import io.horizontalsystems.bankwallet.core.storage.EvmAccountStateDao
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmAccountState
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.CoinType
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
            val blockNumber = fullTransaction.receiptWithLogs?.receipt?.blockNumber ?: 0

            if (blockNumber <= lastBlockNumber) continue

            maxBlockNumber = max(maxBlockNumber, blockNumber)

            if (fullTransaction.transaction.to == address) {
                coinTypes.add(blockchain.baseCoinType)
                continue
            }

            if (fullTransaction.internalTransactions.any { it.to == address }) {
                coinTypes.add(blockchain.baseCoinType)
                continue
            }

            for (decoration in fullTransaction.eventDecorations) {
                (decoration as? TransferEventDecoration)?.let {
                    if (decoration.to == address) {
                        coinTypes.add(blockchain.getEvm20CoinType(decoration.contractAddress.hex))
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
