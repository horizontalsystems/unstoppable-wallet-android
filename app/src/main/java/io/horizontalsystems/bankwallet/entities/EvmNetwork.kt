package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.managers.url
import io.horizontalsystems.ethereumkit.core.EthereumKit

data class EvmNetwork(
    val name: String,
    val networkType: EthereumKit.NetworkType,
    val syncSource: EthereumKit.SyncSource
) {
    val id: String
        get() {
            val syncSourceUrl = syncSource.url.toString()

            return "${networkType.chainId}|$syncSourceUrl"
        }
}
