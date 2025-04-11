package cash.p.terminal.entities.transactionrecords.binancechain

import cash.p.terminal.core.adapters.BinanceAdapter
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.binancechainkit.models.TransactionInfo

class BinanceChainTransactionRecord(
    transaction: TransactionInfo,
    val token: Token,
    feeToken: Token,
    source: TransactionSource,
    val sentToSelf: Boolean = false,
    val value: TransactionValue.CoinValue,
    transactionRecordType: TransactionRecordType,
    val to: String? =  if(transactionRecordType == TransactionRecordType.BINANCE_OUTGOING) transaction.to else null,
    val from: String? = if(transactionRecordType == TransactionRecordType.BINANCE_INCOMING) transaction.from else null
) : TransactionRecord(
    uid = transaction.hash,
    transactionHash = transaction.hash,
    transactionIndex = 0,
    blockHeight = transaction.blockNumber,
    confirmationsThreshold = BinanceAdapter.confirmationsThreshold,
    timestamp = transaction.date.time / 1000,
    failed = false,
    source = source,
    transactionRecordType = transactionRecordType
) {

    val fee = TransactionValue.CoinValue(feeToken, BinanceAdapter.transferFee)
    val memo = transaction.memo

}


