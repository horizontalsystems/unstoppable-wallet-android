package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.EvmAccountStateDao
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.EvmAccountState
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsEip20Provider
import io.reactivex.disposables.CompositeDisposable

class AutoEnableTokensService(
    private val kitManager: EvmKitManager,
    private val walletActivator: WalletActivator,
    private val enableCoinsEip20Provider: EnableCoinsEip20Provider,
    private val evmAccountStateDao: EvmAccountStateDao
) {
    private val disposables = CompositeDisposable()

    fun start() {
        kitManager.kitStartedObservable
            .subscribeIO { started ->
                if (started) {
                    enableTokensWithTx()
                }
            }
            .let {
                disposables.add(it)
            }
    }

    private fun enableTokensWithTx() {
        val account = kitManager.currentAccount ?: return
        val evmKit = kitManager.evmKitWrapper?.evmKit ?: return

        val address = evmKit.receiveAddress.hex
        val chainId = evmKit.networkType.chainId

        val evmAccountState = evmAccountStateDao.get(account.id, chainId) ?: EvmAccountState(account.id, chainId, 0)

        enableCoinsEip20Provider.getCoinTypesAsync(address, evmAccountState.transactionsSyncedBlockNumber)
            .subscribeIO { (coinTypes, lastTxBlockNumber) ->
                val notEnabled = coinTypes.filter { !walletActivator.isEnabled(account, it) }
                if (notEnabled.isNotEmpty()) {
                    walletActivator.activateWallets(account, notEnabled)
                }

                lastTxBlockNumber?.let {
                    evmAccountStateDao.insert(evmAccountState.copy(transactionsSyncedBlockNumber = it))
                }
            }
            .let {
                disposables.add(it)
            }
    }
}
