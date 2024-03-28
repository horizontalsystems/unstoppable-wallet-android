package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.ethereumkit.models.TransactionData

sealed class SendTransactionData {
    data class Evm(val transactionData: TransactionData, val gasLimit: Long?): SendTransactionData()
}
