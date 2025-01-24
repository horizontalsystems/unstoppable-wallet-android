package cash.p.terminal.modules.multiswap.sendtransaction

import cash.p.terminal.wallet.Token
import io.horizontalsystems.ethereumkit.models.TransactionData
import java.math.BigDecimal

sealed class SendTransactionData {
    data class Common(val amount: BigDecimal, val address: String, val token: Token): SendTransactionData()
    data class Evm(val transactionData: TransactionData, val gasLimit: Long?): SendTransactionData()
}
