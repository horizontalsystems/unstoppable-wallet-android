package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.managers.urls
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource

data class EvmNetwork(
    val name: String,
    val chain: Chain,
    val rpcSource: RpcSource
) {
    val id: String
        get() {
            val syncSourceUrl = rpcSource.urls.joinToString(separator = ",") { it.toString() }

            return "${chain.id}|$syncSourceUrl"
        }
}
