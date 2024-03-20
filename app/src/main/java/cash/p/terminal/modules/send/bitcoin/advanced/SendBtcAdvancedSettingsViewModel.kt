package cash.p.terminal.modules.send.bitcoin.advanced

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsModule.SortModeViewItem
import io.horizontalsystems.marketkit.models.BlockchainType

class SendBtcAdvancedSettingsViewModel(
    val blockchainType: BlockchainType,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val localStorage: ILocalStorage,
) : ViewModelUiState<SendBtcAdvancedSettingsModule.UiState>() {

    private var sortMode = btcBlockchainManager.transactionSortMode(blockchainType)
    private val sortOptions: List<SortModeViewItem>
        get() = getTransactionSortModeViewItems()
    private var utxoExpertModeEnabled = localStorage.utxoExpertModeEnabled
    private var rbfEnabled = localStorage.rbfEnabled

    override fun createState() = SendBtcAdvancedSettingsModule.UiState(
        transactionSortOptions = sortOptions,
        transactionSortTitle = Translator.getString(sortMode.titleShort),
        utxoExpertModeEnabled = utxoExpertModeEnabled,
        rbfEnabled = rbfEnabled
    )

    fun setTransactionMode(mode: TransactionDataSortMode) {
        sortMode = mode
        btcBlockchainManager.save(sortMode, blockchainType)
        emitState()
    }

    fun setUtxoExpertMode(enabled: Boolean) {
        utxoExpertModeEnabled = enabled
        localStorage.utxoExpertModeEnabled = enabled
        emitState()
    }

    fun setRbfEnabled(enabled: Boolean) {
        rbfEnabled = enabled
        localStorage.rbfEnabled = enabled
        emitState()
    }

    private fun getTransactionSortModeViewItems(): List<SortModeViewItem> {
        return TransactionDataSortMode.values().map { mode ->
            SortModeViewItem(
                mode = mode,
                selected = mode == sortMode
            )
        }
    }

    fun reset() {
        setTransactionMode(TransactionDataSortMode.Shuffle)
        setRbfEnabled(true)
    }
}
