package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
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

    val restoreModes: List<BtcRestoreMode>
        get() = btcBlockchainManager.availableRestoreModes(blockchain.type)

    fun save() {
        if (restoreMode != btcBlockchainManager.restoreMode(blockchain.type)) {
            btcBlockchainManager.save(restoreMode, blockchain.type)

            stat(page = StatPage.BlockchainSettingsBtc, event = StatEvent.SwitchBtcSource(blockchain.uid, restoreMode))
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
