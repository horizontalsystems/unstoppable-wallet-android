package cash.p.terminal.entities.transactionrecords.evm

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction

class UnknownSwapTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val exchangeAddress: String,
    val valueIn: TransactionValue?,
    val valueOut: TransactionValue?,
) : EvmTransactionRecord(transaction, baseToken, source)
