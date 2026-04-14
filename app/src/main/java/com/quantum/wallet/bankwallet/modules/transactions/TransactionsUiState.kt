package com.quantum.wallet.bankwallet.modules.transactions

import com.quantum.wallet.bankwallet.entities.ViewState

data class TransactionsUiState(
    val transactions: Map<String, List<TransactionViewItem>>?,
    val viewState: ViewState,
    val transactionListId: String?,
    val syncing: Boolean
)
