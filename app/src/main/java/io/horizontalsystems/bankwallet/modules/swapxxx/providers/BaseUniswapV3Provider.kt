package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.bankwallet.modules.swapxxx.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.swapxxx.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swapxxx.SwapQuoteUniswapV3
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataField
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapFeeField
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.DexType
import java.math.BigDecimal

abstract class BaseUniswapV3Provider(dexType: DexType) : ISwapXxxProvider {
    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(dexType) }

    final override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote {
        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val chain = evmBlockchainHelper.chain

        val uniswapTokenFrom = uniswapToken(tokenIn, chain)
        val uniswapTokenTo = uniswapToken(tokenOut, chain)

        val tradeDataV3 = uniswapV3Kit.bestTradeExactIn(
            evmBlockchainHelper.getRpcSourceHttp(),
            chain,
            uniswapTokenFrom,
            uniswapTokenTo,
            amountIn,
            SwapTradeOptions().tradeOptions
        )
        val amountOut = tradeDataV3.tokenAmountOut.decimalAmount!!

        val transactionData = evmBlockchainHelper.receiveAddress?.let { receiveAddress ->
            uniswapV3Kit.transactionData(receiveAddress, chain, tradeDataV3)
        }
        val feeAmountData = transactionData?.let {
            evmBlockchainHelper.getFeeAmountData(transactionData)
        }

        val fields = buildList<SwapDataField> {
            feeAmountData?.let {
                add(SwapFeeField(feeAmountData))
            }
        }

        return SwapQuoteUniswapV3(amountOut, fields, feeAmountData, blockchainType)
    }

    @Throws
    private fun uniswapToken(token: Token?, chain: Chain) = when (val tokenType = token?.type) {
        TokenType.Native -> when (token.blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> uniswapV3Kit.etherToken(chain)
            else -> throw Exception("Invalid coin for swap: $token")
        }
        is TokenType.Eip20 -> uniswapV3Kit.token(
            io.horizontalsystems.ethereumkit.models.Address(
                tokenType.address
            ), token.decimals)
        else -> throw Exception("Invalid coin for swap: $token")
    }
}
