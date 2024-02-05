package io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsModule.SortModeViewItem
import io.horizontalsystems.marketkit.models.BlockchainType

class SendBtcAdvancedSettingsViewModel(
    val blockchainType: BlockchainType,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val localStorage: ILocalStorage,
) : ViewModel() {

    private var sortMode = btcBlockchainManager.transactionSortMode(blockchainType)
    private val sortOptions: List<SortModeViewItem>
        get() = getTransactionSortModeViewItems()
    private var utxoExpertModeEnabled = localStorage.utxoExpertModeEnabled
    private var rbfEnabled = localStorage.rbfEnabled

    var uiState by mutableStateOf(
        SendBtcAdvancedSettingsModule.UiState(
            transactionSortOptions = sortOptions,
            transactionSortTitle = Translator.getString(sortMode.titleShort),
            utxoExpertModeEnabled = utxoExpertModeEnabled,
            rbfEnabled = rbfEnabled,
        )
    )

    fun setTransactionMode(mode: TransactionDataSortMode) {
        sortMode = mode
        btcBlockchainManager.save(sortMode, blockchainType)
        syncState()
    }

    fun setUtxoExpertMode(enabled: Boolean) {
        utxoExpertModeEnabled = enabled
        localStorage.utxoExpertModeEnabled = enabled
        syncState()
    }

    fun setRbfEnabled(enabled: Boolean) {
        rbfEnabled = enabled
        localStorage.rbfEnabled = enabled
        syncState()
    }

    private fun syncState() {
        uiState = SendBtcAdvancedSettingsModule.UiState(
            transactionSortOptions = sortOptions,
            transactionSortTitle = Translator.getString(sortMode.titleShort),
            utxoExpertModeEnabled = utxoExpertModeEnabled,
            rbfEnabled = rbfEnabled
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

    fun reset() {
        setTransactionMode(TransactionDataSortMode.Shuffle)
        setRbfEnabled(true)
    }
}
