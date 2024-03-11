package cash.p.terminal.modules.swapxxx.sendtransaction

import io.horizontalsystems.ethereumkit.models.TransactionData

sealed class SendTransactionData {
    data class Evm(val transactionData: TransactionData): SendTransactionData()
}
