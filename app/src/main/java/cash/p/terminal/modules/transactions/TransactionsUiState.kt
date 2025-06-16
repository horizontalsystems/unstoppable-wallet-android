package cash.p.terminal.modules.transactions

import cash.p.terminal.ui_compose.entities.ViewState

data class TransactionsUiState(
    val transactions: Map<String, List<TransactionViewItem>>?,
    val viewState: ViewState,
    val transactionListId: String?,
    val syncing: Boolean,
    val hasHiddenTransactions: Boolean
)
