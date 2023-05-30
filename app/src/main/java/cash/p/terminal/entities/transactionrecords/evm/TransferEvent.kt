package cash.p.terminal.entities.transactionrecords.evm

import cash.p.terminal.entities.TransactionValue

data class TransferEvent(
    val address: String?,
    val value: TransactionValue
)
