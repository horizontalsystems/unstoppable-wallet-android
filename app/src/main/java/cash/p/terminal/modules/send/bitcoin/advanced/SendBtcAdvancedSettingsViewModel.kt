package cash.p.terminal.modules.send.bitcoin.advanced

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsModule.SortModeViewItem
import io.horizontalsystems.marketkit.models.BlockchainType

class SendBtcAdvancedSettingsViewModel(
    val blockchainType: BlockchainType,
    private val btcBlockchainManager: BtcBlockchainManager,
) : ViewModel() {

    private var sortMode = btcBlockchainManager.transactionSortMode(blockchainType)
    private val sortOptions: List<SortModeViewItem>
        get() = getTransactionSortModeViewItems()

    var uiState by mutableStateOf(
        SendBtcAdvancedSettingsModule.UiState(
            transactionSortOptions = sortOptions,
            transactionSortTitle = Translator.getString(sortMode.titleShort)
        )
    )

    fun setTransactionMode(mode: TransactionDataSortMode) {
        sortMode = mode
        btcBlockchainManager.save(sortMode, blockchainType)
        syncState()
    }

    private fun syncState() {
        uiState = SendBtcAdvancedSettingsModule.UiState(
            transactionSortOptions = sortOptions,
            transactionSortTitle = Translator.getString(sortMode.titleShort)
        )
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
