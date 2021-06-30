package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

class EvmOutgoingTransactionRecord(
        fullTransaction: FullTransaction,
        val amount: BigDecimal,
        val to: String,
        val token: Coin,
        val sentToSelf: Boolean
): EvmTransactionRecord(fullTransaction) {

    override val mainCoin: Coin = token
    override val mainAmount: BigDecimal = amount

    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        val coinValue = CoinValue(token, amount)
        return TransactionType.Outgoing(to, coinValue, null, null, sentToSelf)
    }
}
