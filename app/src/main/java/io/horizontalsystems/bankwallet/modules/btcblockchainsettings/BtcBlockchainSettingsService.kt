package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class BtcBlockchainSettingsService(
    val blockchain: Blockchain,
    private val btcBlockchainManager: BtcBlockchainManager
) {

    private val hasChangesSubject = BehaviorSubject.create<Boolean>()
    val hasChangesObservable: Observable<Boolean>
        get() = hasChangesSubject

    var restoreMode: BtcRestoreMode = btcBlockchainManager.restoreMode(blockchain.type)
        private set

    var transactionMode: TransactionDataSortMode =
        btcBlockchainManager.transactionSortMode(blockchain.type)
        private set

    fun save() {
        if (restoreMode != btcBlockchainManager.restoreMode(blockchain.type)) {
            btcBlockchainManager.save(restoreMode, blockchain.type)
        }

        if (transactionMode != btcBlockchainManager.transactionSortMode(blockchain.type)) {
            btcBlockchainManager.save(transactionMode, blockchain.type)
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
        val initialRestoreMode = btcBlockchainManager.restoreMode(blockchain.type)
        val initialTransactionMode = btcBlockchainManager.transactionSortMode(blockchain.type)

        hasChangesSubject.onNext(restoreMode != initialRestoreMode || transactionMode != initialTransactionMode)
    }
}
