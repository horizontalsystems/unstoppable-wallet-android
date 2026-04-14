package com.quantum.wallet.bankwallet.modules.multiswap.providers

import com.quantum.wallet.bankwallet.R
import io.horizontalsystems.marketkit.models.BlockchainType

object QuickSwapProvider : BaseUniswapProvider() {
    override val id = "quickswap"
    override val title = "QuickSwap"
    override val icon = R.drawable.swap_provider_quickswap
    override val riskLevel = RiskLevel.CONTROLLED

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Polygon
    }
}
