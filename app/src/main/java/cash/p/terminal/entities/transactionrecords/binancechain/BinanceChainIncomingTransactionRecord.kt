package cash.p.terminal.entities.transactionrecords.binancechain

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import cash.p.terminal.wallet.transaction.TransactionSource

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
