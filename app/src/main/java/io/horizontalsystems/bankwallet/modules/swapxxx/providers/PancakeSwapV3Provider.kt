package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.R
import cash.p.terminal.modules.swap.ISwapQuote
import cash.p.terminal.modules.swap.uniswapv3.UniswapV3TradeService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.models.DexType
import java.math.BigDecimal

object PancakeSwapV3Provider : ISwapXxxProvider {
    override val id = "pancake_v3"
    override val title = "PancakeSwap V3"
    override val url = "https://pancakeswap.finance/"
    override val icon = R.drawable.pancake_v3
    private val service = UniswapV3TradeService(DexType.PancakeSwap)

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.BinanceSmartChain,
        BlockchainType.Ethereum
        -> true
        else -> false
    }

    override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): ISwapQuote {
        return service.fetchQuote(tokenIn, tokenOut, amountIn)
    }
}
