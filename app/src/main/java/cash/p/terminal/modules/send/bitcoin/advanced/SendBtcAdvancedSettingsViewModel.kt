package cash.p.terminal.modules.send.bitcoin.advanced

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsModule.SortModeViewItem
import io.horizontalsystems.marketkit.models.BlockchainType

class SendBtcAdvancedSettingsViewModel(
    val blockchainType: BlockchainType,
    private val btcBlockchainManager: BtcBlockchainManager,
) : ViewModel() {

    private var sortMode = btcBlockchainManager.transactionSortMode(blockchainType)

    var sortModeViewItems by mutableStateOf(getTransactionSortModeViewItems())

    fun setTransactionMode(mode: TransactionDataSortMode) {
        sortMode = mode
        btcBlockchainManager.save(sortMode, blockchainType)
        sortModeViewItems = getTransactionSortModeViewItems()
    }

    private fun getTransactionSortModeViewItems(): List<SortModeViewItem> {
        return TransactionDataSortMode.values().map { mode ->
            SortModeViewItem(
                mode = mode,
                selected = mode == sortMode
            )
        }
    }
}
