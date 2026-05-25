package io.horizontalsystems.bankwallet.modules.walletconnect.request

import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.marketkit.models.BlockchainType
import javax.inject.Inject

@HiltViewModel
class WCRequestRouterViewModel @Inject constructor(
    private val evmBlockchainManager: EvmBlockchainManager
) : ViewModelUiState<WCRequestRouterUiState>() {
    private val sessionRequest = WCDelegate.sessionRequestEvent
    private val blockchainType = determineBlockchainType()

    private fun determineBlockchainType(): BlockchainType? {
        val chainParts = sessionRequest?.chainId?.split(":") ?: return null
        val first = chainParts[0]

        return when (first) {
            "eip155" -> {
                val chainId = chainParts[1].toIntOrNull()
                chainId?.let {
                    evmBlockchainManager.getBlockchain(it)
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
}

data class WCRequestRouterUiState(val blockchainType: BlockchainType?)