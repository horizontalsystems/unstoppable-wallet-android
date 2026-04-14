package com.quantum.wallet.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.modules.walletconnect.WCDelegate
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