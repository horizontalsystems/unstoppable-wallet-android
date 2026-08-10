package io.horizontalsystems.walletkit.modules.walletconnect

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.modules.walletconnect.request.WCChainData
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType

object WCUtils {
    fun getChainData(string: String?): WCChainData? {
        if (string == null) return null
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
            WCChainData(chain.id, chain.name, address)
        }
    }

    fun getBlockchainType(sessionChainId: String?): BlockchainType? {
        val chainId = getChainData(sessionChainId)?.id
        return chainId?.let { App.evmBlockchainManager.getBlockchain(it) }?.type
    }
}
