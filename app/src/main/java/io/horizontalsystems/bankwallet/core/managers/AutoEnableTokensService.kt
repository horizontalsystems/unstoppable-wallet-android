package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.EvmAccountStateDao
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.EvmAccountState
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsEip20Provider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

class AutoEnableTokensService(
    private val kitManager: EvmKitManager,
    private val walletActivator: WalletActivator,
    private val enableCoinsEip20Provider: EnableCoinsEip20Provider,
    private val evmAccountStateDao: EvmAccountStateDao
) {
    private val disposables = CompositeDisposable()
    private var transactionsDisposable: Disposable? = null

    fun start() {
        kitManager.kitStartedObservable
            .subscribeIO { started ->
                if (started) {
                    enableTokensWithTx()

                    transactionsDisposable?.dispose()
                    transactionsDisposable = kitManager.evmKitWrapper?.evmKit
                        ?.getTransactionsFlowable(listOf())
                        ?.subscribeIO {
                            val lastBlockNumber = it.maxOf {
                                it.receiptWithLogs?.receipt?.blockNumber ?: 0
                            }

                            enableTokensWithTx(lastBlockNumber)
                        }
                } else {
                    transactionsDisposable?.dispose()
                }
            }
            .let {
                disposables.add(it)
            }
    }

    private fun enableTokensWithTx(lastBlockNumber: Long = 0) {
        val account = kitManager.currentAccount ?: return
        val evmKit = kitManager.evmKitWrapper?.evmKit ?: return

        val chainId = evmKit.networkType.chainId
        val evmAccountState = evmAccountStateDao.get(account.id, chainId) ?: EvmAccountState(account.id, chainId, -1)

        if (evmAccountState.transactionsSyncedBlockNumber >= lastBlockNumber) return

        val address = evmKit.receiveAddress.hex
        enableCoinsEip20Provider.getCoinTypesAsync(address, evmAccountState.transactionsSyncedBlockNumber + 1)
            .subscribeIO { (coinTypes, lastBlockNumber) ->
                val notEnabled = coinTypes.filter { !walletActivator.isEnabled(account, it) }
                if (notEnabled.isNotEmpty()) {
                    walletActivator.activateWallets(account, notEnabled)
                }

                evmAccountStateDao.insert(evmAccountState.copy(transactionsSyncedBlockNumber = lastBlockNumber))
            }
            .let {
                disposables.add(it)
            }
    }
}
