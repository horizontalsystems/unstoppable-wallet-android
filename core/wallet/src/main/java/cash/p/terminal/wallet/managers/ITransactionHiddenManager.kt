package cash.p.terminal.wallet.managers

import kotlinx.coroutines.flow.StateFlow

interface ITransactionHiddenManager {
    val transactionHiddenFlow: StateFlow<TransactionHiddenState>
    fun showAllTransactions(show: Boolean)
    fun setTransactionHideEnabled(enabled: Boolean)
    fun setTransactionDisplayLevel(displayLevel: TransactionDisplayLevel)
    fun setSeparatePin(pin: String)
    fun clearSeparatePin()
    fun isPinMatches(pin: String): Boolean
}

data class TransactionHiddenState(
    val transactionHidden: Boolean,
    val transactionHideEnabled: Boolean,
    val transactionDisplayLevel: TransactionDisplayLevel,
    val transactionAutoHidePinExists: Boolean
)

enum class TransactionDisplayLevel {
    NOTHING,
    LAST_4_TRANSACTIONS,
    LAST_2_TRANSACTIONS,
    LAST_1_TRANSACTION;
}
