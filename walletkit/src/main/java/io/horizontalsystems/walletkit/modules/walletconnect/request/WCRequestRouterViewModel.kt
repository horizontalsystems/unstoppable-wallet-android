package io.horizontalsystems.walletkit.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.modules.walletconnect.solanaMainnetReferences
import io.horizontalsystems.marketkit.models.BlockchainType

class WCRequestRouterViewModel : ViewModelUiState<WCRequestRouterUiState>() {
    private val sessionRequest = WCDelegate.sessionRequestEvent
    private val blockchainType = determineBlockchainType()

    private fun determineBlockchainType(): BlockchainType? {
        val chainParts = sessionRequest?.chainId?.split(":") ?: return null
        val first = chainParts[0]

        return when (first) {
            "eip155" -> {
                val chainId = chainParts[1].toIntOrNull()
                chainId?.let {
                    App.evmBlockchainManager.getBlockchain(it)
                }?.type
            }

            "stellar" -> {
                val s = chainParts[1]
                if (s == "pubnet") {
                    BlockchainType.Stellar
                } else {
                    null
                }
            }

            "solana" -> {
                // Only the supported mainnet-beta references (canonical genesis-hash value + the
                // legacy CAIP-30 value some dApps still use); reject devnet/testnet or any other
                // cluster so an unsupported Solana network is never routed as Solana.
                if (chainParts.getOrNull(1) in solanaMainnetReferences) BlockchainType.Solana else null
            }

            else -> null
        }
    }

    override fun createState() = WCRequestRouterUiState(
        blockchainType = blockchainType
    )

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestRouterViewModel() as T
        }
    }
}

data class WCRequestRouterUiState(val blockchainType: BlockchainType?)