package cash.p.terminal.entities.transactionrecords.binancechain

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.horizontalsystems.marketkit.models.Token

class BinanceChainIncomingTransactionRecord(
    transaction: TransactionInfo,
    feeToken: Token,
    token: Token,
    source: TransactionSource
) : BinanceChainTransactionRecord(transaction, feeToken, source) {
    val value = TransactionValue.CoinValue(token, transaction.amount.toBigDecimal())
    val from = transaction.from

    override val mainValue = value

}
