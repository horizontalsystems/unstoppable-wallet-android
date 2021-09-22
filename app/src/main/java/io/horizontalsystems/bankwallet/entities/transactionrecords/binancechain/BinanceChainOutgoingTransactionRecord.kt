package io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.horizontalsystems.marketkit.models.PlatformCoin

class BinanceChainOutgoingTransactionRecord(
    transaction: TransactionInfo,
    feeCoin: PlatformCoin,
    coin: PlatformCoin,
    val sentToSelf: Boolean,
    source: TransactionSource
) : BinanceChainTransactionRecord(transaction, feeCoin, source) {
    val value = TransactionValue.CoinValue(coin, transaction.amount.toBigDecimal())
    val to = transaction.to

    override val mainValue = value

}
