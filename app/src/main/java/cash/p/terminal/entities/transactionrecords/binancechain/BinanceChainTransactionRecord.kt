package cash.p.terminal.entities.transactionrecords.binancechain

import cash.p.terminal.core.adapters.BinanceAdapter
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.horizontalsystems.marketkit.models.Token

abstract class BinanceChainTransactionRecord(
    transaction: TransactionInfo,
    feeToken: Token,
    source: TransactionSource
) : TransactionRecord(
    uid = transaction.hash,
    transactionHash = transaction.hash,
    transactionIndex = 0,
    blockHeight = transaction.blockNumber,
    confirmationsThreshold = BinanceAdapter.confirmationsThreshold,
    timestamp = transaction.date.time / 1000,
    failed = false,
    source = source
) {

    val fee = TransactionValue.CoinValue(feeToken, BinanceAdapter.transferFee)
    val memo = transaction.memo

}
