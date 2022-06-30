package io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
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
