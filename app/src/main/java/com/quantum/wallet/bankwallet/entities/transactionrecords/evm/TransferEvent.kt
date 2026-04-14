package com.quantum.wallet.bankwallet.entities.transactionrecords.evm

import com.quantum.wallet.bankwallet.entities.TransactionValue

data class TransferEvent(
    val address: String?,
    val value: TransactionValue
)
