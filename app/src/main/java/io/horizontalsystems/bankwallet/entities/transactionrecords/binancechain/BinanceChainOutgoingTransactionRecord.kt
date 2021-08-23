package io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.horizontalsystems.coinkit.models.Coin

class BinanceChainOutgoingTransactionRecord(
    transaction: TransactionInfo,
    feeCoin: Coin,
    coin: Coin,
    val sentToSelf: Boolean,
    source: TransactionSource
) : BinanceChainTransactionRecord(transaction, feeCoin, source) {
    val value = CoinValue(coin, transaction.amount.toBigDecimal())
    val to = transaction.to

    override val mainValue: CoinValue = value

}
