package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import java.net.URI

data class EvmSyncSource(
    val id: String,
    val name: String,
    val rpcSource: RpcSource,
    val transactionSource: TransactionSource
) {
    val isHttp: Boolean = rpcSource is RpcSource.Http

    val uri: URI
        get() = when (val source = rpcSource) {
            is RpcSource.Http -> source.uris[0]
            is RpcSource.WebSocket -> source.uri
        }

    val auth: String?
        get() = when (val source = rpcSource) {
            is RpcSource.Http -> source.auth
            is RpcSource.WebSocket -> source.auth
        }
}

@Entity(primaryKeys = ["blockchainTypeUid", "url"])
data class EvmSyncSourceRecord(
    val blockchainTypeUid: String,
    val url: String,
    val auth: String?,
)
