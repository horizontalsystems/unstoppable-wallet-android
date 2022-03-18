package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource

data class EvmSyncSource(
    val id: String,
    val name: String,
    val rpcSource: RpcSource,
    val transactionSource: TransactionSource
)
