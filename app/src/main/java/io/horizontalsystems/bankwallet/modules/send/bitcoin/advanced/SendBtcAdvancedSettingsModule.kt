package io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced

import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode

object SendBtcAdvancedSettingsModule {

    data class UiState(
        val transactionSortOptions: List<SortModeViewItem>,
        val transactionSortTitle: String,
        val utxoExpertModeEnabled: Boolean,
        val transactionSortingSupported: Boolean,
        val rbfEnabled: Boolean,
        val rbfVisible: Boolean,
    )

    data class SortModeViewItem(
        val mode: TransactionDataSortMode,
        val selected: Boolean,
    )
}
