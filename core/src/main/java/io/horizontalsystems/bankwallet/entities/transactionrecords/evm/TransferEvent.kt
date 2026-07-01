package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue

data class TransferEvent(
    val address: String?,
    val value: TransactionValue
)
