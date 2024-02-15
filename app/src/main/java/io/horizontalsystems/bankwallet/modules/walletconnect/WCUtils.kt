package io.horizontalsystems.bankwallet.modules.walletconnect

import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCChainData
import io.horizontalsystems.ethereumkit.models.Chain

object WCUtils {
    fun getChainData(string: String): WCChainData? {
        val chunks = string.split(":")
        if (chunks.size < 2) {
            return null
        }
        val eip = chunks[0]
        if (eip != "eip155") return null

        val chainId = chunks[1].toIntOrNull() ?: return null
        val chain = Chain.values().firstOrNull { it.id == chainId }
        val address: String? = when {
            chunks.size >= 3 -> chunks[2]
            else -> null
        }

        return chain?.let {
            WCChainData(chain, address)
        }
    }
}