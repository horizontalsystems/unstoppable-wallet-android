package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.modules.swap.EvmBlockchainHelper
import cash.p.terminal.modules.swap.ISwapQuote
import cash.p.terminal.modules.swap.SwapQuoteUniswap
import cash.p.terminal.modules.swap.settings.uniswap.SwapTradeOptions
import cash.p.terminal.modules.swapxxx.ui.SwapDataField
import cash.p.terminal.modules.swapxxx.ui.SwapFeeField
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

abstract class BaseUniswapProvider : ISwapXxxProvider {
    private val uniswapKit by lazy { UniswapKit.getInstance() }

    final override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): ISwapQuote {
        val evmBlockchainHelper = EvmBlockchainHelper(tokenIn.blockchainType)

        val swapData = swapDataSingle(tokenIn, tokenOut, evmBlockchainHelper).await()
        val tradeData = uniswapKit.bestTradeExactIn(swapData, amountIn, SwapTradeOptions().tradeOptions)
        val transactionData = evmBlockchainHelper.receiveAddress?.let { receiveAddress ->
            uniswapKit.transactionData(
                receiveAddress,
                evmBlockchainHelper.chain,
                tradeData
            )
        }
        val feeAmountData = transactionData?.let {
            evmBlockchainHelper.getFeeAmountData(it)
        }

        val fields = buildList<SwapDataField> {
            feeAmountData?.let {
                add(SwapFeeField(feeAmountData))
            }
        }

        return SwapQuoteUniswap(tradeData.amountOut!!, fields, feeAmountData)
    }

    private fun swapDataSingle(
        tokenIn: Token,
        tokenOut: Token,
        evmBlockchainHelper: EvmBlockchainHelper
    ): Single<SwapData> {
        return try {
            val chain = evmBlockchainHelper.chain

            uniswapKit.swapData(
                evmBlockchainHelper.getRpcSourceHttp(),
                chain,
                uniswapToken(tokenIn, chain),
                uniswapToken(tokenOut, chain)
            )
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    @Throws
    private fun uniswapToken(token: Token?, chain: Chain) = when (val tokenType = token?.type) {
        TokenType.Native -> uniswapKit.etherToken(chain)
        is TokenType.Eip20 -> {
            uniswapKit.token(Address(tokenType.address), token.decimals)
        }

        else -> throw Exception("Invalid coin for swap: $token")
    }
}
