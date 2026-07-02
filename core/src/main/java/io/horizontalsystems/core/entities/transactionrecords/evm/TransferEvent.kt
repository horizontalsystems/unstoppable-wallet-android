package io.horizontalsystems.core.entities.transactionrecords.evm

import io.horizontalsystems.core.entities.TransactionValue

data class TransferEvent(
    val address: String?,
    val value: TransactionValue
)
