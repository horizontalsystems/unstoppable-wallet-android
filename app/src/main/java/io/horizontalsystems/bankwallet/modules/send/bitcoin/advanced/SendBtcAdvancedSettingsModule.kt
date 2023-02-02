package io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.modules.send.bitcoin.*
import io.horizontalsystems.marketkit.models.BlockchainType

object SendBtcAdvancedSettingsModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val blockchainType: BlockchainType) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendBtcAdvancedSettingsViewModel(blockchainType, App.btcBlockchainManager) as T
        }
    }

    data class SortModeViewItem(
        val mode: TransactionDataSortMode,
        val selected: Boolean,
    )
}
