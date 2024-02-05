package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapV2TradeService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

object QuickSwapProvider : ISwapXxxProvider {
    override val id = "quickswap"
    override val title = "QuickSwap"
    override val url = "https://quickswap.exchange/"
    override val icon = R.drawable.quickswap
    private val service = UniswapV2TradeService()

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Polygon
    }

    override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): ISwapQuote {
        return service.fetchQuote(tokenIn, tokenOut, amountIn)
    }
}
