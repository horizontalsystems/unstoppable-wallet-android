package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
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

    fun save() {
        if (restoreMode != btcBlockchainManager.restoreMode(blockchain.type)) {
            btcBlockchainManager.save(restoreMode, blockchain.type)
        }
    }

    fun setRestoreMode(id: String) {
        restoreMode = BtcRestoreMode.values().first { it.raw == id }
        syncHasChanges()
    }

    private fun syncHasChanges() {
        val initialRestoreMode = btcBlockchainManager.restoreMode(blockchain.type)
        hasChangesSubject.onNext(restoreMode != initialRestoreMode)
    }
}
