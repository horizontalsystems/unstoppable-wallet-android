package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.entities.BtcBlockchain
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class BtcBlockchainSettingsService(
    val blockchain: BtcBlockchain,
    private val btcBlockchainManager: BtcBlockchainManager
) {

    private val hasChangesSubject = BehaviorSubject.create<Boolean>()
    val hasChangesObservable: Observable<Boolean>
        get() = hasChangesSubject

    var restoreMode: BtcRestoreMode = btcBlockchainManager.restoreMode(blockchain)
        private set

    var transactionMode: TransactionDataSortMode =
        btcBlockchainManager.transactionSortMode(blockchain)
        private set

    fun save() {
        if (restoreMode != btcBlockchainManager.restoreMode(blockchain)) {
            btcBlockchainManager.save(restoreMode, blockchain)
        }

        if (transactionMode != btcBlockchainManager.transactionSortMode(blockchain)) {
            btcBlockchainManager.save(transactionMode, blockchain)
        }
    }

    fun setRestoreMode(id: String) {
        restoreMode = BtcRestoreMode.values().first { it.raw == id }
        syncHasChanges()
    }

    fun setTransactionMode(id: String) {
        transactionMode = TransactionDataSortMode.values().first { it.raw == id }
        syncHasChanges()
    }

    private fun syncHasChanges() {
        val initialRestoreMode = btcBlockchainManager.restoreMode(blockchain)
        val initialTransactionMode = btcBlockchainManager.transactionSortMode(blockchain)

        hasChangesSubject.onNext(restoreMode != initialRestoreMode || transactionMode != initialTransactionMode)
    }
}
