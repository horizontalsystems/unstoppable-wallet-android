package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.R
import io.horizontalsystems.core.entities.BlockchainType

object QuickSwapProvider : BaseUniswapProvider() {
    override val id = "quickswap"
    override val title = "QuickSwap"
    override val url = "https://quickswap.exchange/"
    override val icon = R.drawable.quickswap
    override val priority = 0

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Polygon
    }
}
