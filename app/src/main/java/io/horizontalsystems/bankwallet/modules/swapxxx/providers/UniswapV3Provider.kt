package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swap.uniswapv3.UniswapV3TradeService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.models.DexType
import java.math.BigDecimal

object UniswapV3Provider : ISwapXxxProvider {
    override val id = "uniswap_v3"
    override val title = "Uniswap V3"
    override val url = "https://uniswap.org/"
    override val icon = R.drawable.uniswap_v3
    private val service = UniswapV3TradeService(DexType.Uniswap)

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.ArbitrumOne,
//            BlockchainType.Optimism,
        BlockchainType.Polygon,
        BlockchainType.BinanceSmartChain
        -> true
        else -> false
    }

    override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): ISwapQuote {
        return service.fetchQuote(tokenIn, tokenOut, amountIn)
    }
}
