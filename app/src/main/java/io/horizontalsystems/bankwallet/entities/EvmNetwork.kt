package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.managers.urls
import io.horizontalsystems.ethereumkit.core.EthereumKit

data class EvmNetwork(
    val name: String,
    val networkType: EthereumKit.NetworkType,
    val syncSource: EthereumKit.SyncSource
) {
    val id: String
        get() {
            val syncSourceUrl = syncSource.urls.joinToString(separator = ",") { it.toString() }

            return "${networkType.chainId}|$syncSourceUrl"
        }
}
