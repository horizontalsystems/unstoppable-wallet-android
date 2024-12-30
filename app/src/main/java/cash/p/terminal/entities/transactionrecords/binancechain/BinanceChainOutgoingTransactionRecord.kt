package cash.p.terminal.entities.transactionrecords.binancechain

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.binancechainkit.models.TransactionInfo

class BinanceChainOutgoingTransactionRecord(
    transaction: TransactionInfo,
    feeToken: Token,
    token: Token,
    val sentToSelf: Boolean,
    source: TransactionSource
) : BinanceChainTransactionRecord(transaction, feeToken, source) {
    val value = TransactionValue.CoinValue(token, transaction.amount.toBigDecimal().negate())
    val to = transaction.to

    override val mainValue = value

}
