package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.ethereumkit.models.TransactionData
import java.math.BigDecimal

sealed class SendTransactionData {
    data class Evm(val transactionData: TransactionData, val gasLimit: Long?): SendTransactionData()
    data class Btc(
        val address: String,
        val memo: String,
        val amount: BigDecimal,
        val recommendedGasRate: Int,
        val dustThreshold: Int?,
        val changeToFirstInput: Boolean,
        val utxoFilters: UtxoFilters
    ) : SendTransactionData()
}
