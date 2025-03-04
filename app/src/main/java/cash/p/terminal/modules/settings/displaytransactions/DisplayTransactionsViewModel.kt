package cash.p.terminal.modules.settings.displaytransactions

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import kotlinx.coroutines.flow.map

class DisplayTransactionsViewModel(
    private val transactionHiddenManager: TransactionHiddenManager
) : ViewModel() {

    val uiState = transactionHiddenManager.transactionHiddenFlow.map { it.transactionDisplayLevel }

    fun onItemSelected(selectedItem: TransactionDisplayLevel) {
        transactionHiddenManager.setTransactionDisplayLevel(selectedItem)
    }
}
