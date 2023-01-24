package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import java.net.URL

data class EvmSyncSource(
    val id: String,
    val name: String,
    val rpcSource: RpcSource,
    val transactionSource: TransactionSource
) {
    val isHttp: Boolean = rpcSource is RpcSource.Http

    val url: URL
        get() = when (val source = rpcSource) {
            is RpcSource.Http -> source.urls[0]
            is RpcSource.WebSocket -> source.url
        }
}

@Entity(primaryKeys = ["blockchainTypeUid", "url"])
data class EvmSyncSourceRecord(
    val blockchainTypeUid: String,
    val url: String,
    val auth: String?,
)
