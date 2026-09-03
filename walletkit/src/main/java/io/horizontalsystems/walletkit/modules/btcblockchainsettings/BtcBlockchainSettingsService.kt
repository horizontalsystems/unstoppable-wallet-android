package io.horizontalsystems.walletkit.modules.btcblockchainsettings

import io.horizontalsystems.walletkit.core.managers.BtcBlockchainManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.BtcRestoreMode
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class BtcBlockchainSettingsService(
    val blockchain: Blockchain,
    private val btcBlockchainManager: BtcBlockchainManager
) {

    private val _hasChangesFlow = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val hasChangesFlow: Flow<Boolean>
        get() = _hasChangesFlow

    var restoreMode: BtcRestoreMode = btcBlockchainManager.restoreMode(blockchain.type)
        private set

    val restoreModes: List<BtcRestoreMode>
        get() = btcBlockchainManager.availableRestoreModes(blockchain.type)

    fun save() {
        if (restoreMode != btcBlockchainManager.restoreMode(blockchain.type)) {
            btcBlockchainManager.save(restoreMode, blockchain.type)

            stat(
                page = StatPage.BlockchainSettingsBtc,
                event = StatEvent.SwitchBtcSource(blockchain.uid, restoreMode)
            )
        }
    }

    fun setRestoreMode(id: String) {
        restoreMode = BtcRestoreMode.values().first { it.raw == id }
        syncHasChanges()
    }

    private fun syncHasChanges() {
        val initialRestoreMode = btcBlockchainManager.restoreMode(blockchain.type)
        _hasChangesFlow.tryEmit(restoreMode != initialRestoreMode)
    }
}
