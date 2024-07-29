package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.R
import io.horizontalsystems.marketkit.models.BlockchainType

object PancakeSwapProvider : BaseUniswapProvider() {
    override val id = "pancake"
    override val title = "PancakeSwap"
    override val url = "https://pancakeswap.finance/"
    override val icon = R.drawable.pancake
    override val priority = 0

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.BinanceSmartChain
    }
}
