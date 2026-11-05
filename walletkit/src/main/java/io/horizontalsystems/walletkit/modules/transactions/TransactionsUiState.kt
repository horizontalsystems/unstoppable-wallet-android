package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.entities.ViewState

data class TransactionsUiState(
    val tab: ActivityTab,
    val transactions: Map<String, List<TransactionViewItem>>?,
    val viewState: ViewState,
    val transactionListId: String?,
    val syncing: Boolean
)
