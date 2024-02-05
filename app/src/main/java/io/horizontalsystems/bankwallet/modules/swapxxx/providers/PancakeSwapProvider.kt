package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapV2TradeService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

object PancakeSwapProvider : ISwapXxxProvider {
    override val id = "pancake"
    override val title = "PancakeSwap"
    override val url = "https://pancakeswap.finance/"
    override val icon = R.drawable.pancake
    private val service = UniswapV2TradeService()

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.BinanceSmartChain
    }

    override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): ISwapQuote {
        return service.fetchQuote(tokenIn, tokenOut, amountIn)
    }
}
