package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.models.BlockchainType

object PancakeSwapProvider : BaseUniswapProvider() {
    override val id = "pancake"
    override val title = "PancakeSwap"
    override val icon = R.drawable.swap_provider_pancake
    override val riskLevel = RiskLevel.LIMITED

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.BinanceSmartChain
    }
}
