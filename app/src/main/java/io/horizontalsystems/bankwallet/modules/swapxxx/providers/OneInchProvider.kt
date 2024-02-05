package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.R
import cash.p.terminal.modules.swapxxx.ISwapQuote
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

object OneInchProvider : ISwapXxxProvider {
    override val id = "oneinch"
    override val title = "1inch"
    override val url = "https://app.1inch.io/"
    override val icon = R.drawable.oneinch
//    private val oneInchKitHelper by lazy { OneInchKitHelper(App.appConfigProvider.oneInchApiKey) }

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne
        -> true

        else -> false
    }

    override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): ISwapQuote {
        TODO()
//        val chain = App.evmBlockchainManager.getChain(tokenIn.blockchainType)
//
//        val quote = oneInchKitHelper.getQuoteAsync(chain, tokenIn, tokenOut, amountIn).await()
//        val amountOut = quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
//        return SwapQuoteOneInch(amountOut, listOf(), null, tokenIn.blockchainType)
    }
}
