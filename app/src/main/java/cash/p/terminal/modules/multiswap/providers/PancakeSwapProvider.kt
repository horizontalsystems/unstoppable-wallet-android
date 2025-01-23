package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.R
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.BlockchainType

object PancakeSwapProvider : BaseUniswapProvider() {
    override val id = "pancake"
    override val title = "PancakeSwap"
    override val url = "https://pancakeswap.finance/"
    override val icon = R.drawable.pancake
    override val priority = 0

    override suspend fun supports(token: Token): Boolean {
        return token.blockchainType == BlockchainType.BinanceSmartChain
    }
}
