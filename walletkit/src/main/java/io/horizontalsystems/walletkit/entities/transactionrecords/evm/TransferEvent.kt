package io.horizontalsystems.walletkit.entities.transactionrecords.evm

import io.horizontalsystems.walletkit.entities.TransactionValue

data class TransferEvent(
    val address: String?,
    val value: TransactionValue
)
