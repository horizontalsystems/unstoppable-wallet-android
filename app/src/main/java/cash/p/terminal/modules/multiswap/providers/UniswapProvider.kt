package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.R
import io.horizontalsystems.core.entities.BlockchainType

object UniswapProvider : BaseUniswapProvider() {
    override val id = "uniswap"
    override val title = "Uniswap"
    override val url = "https://uniswap.org/"
    override val icon = R.drawable.uniswap
    override val priority = 0

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Ethereum
    }
}
