package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.modules.swapxxx.EvmBlockchainHelper
import cash.p.terminal.modules.swapxxx.ISwapQuote
import cash.p.terminal.modules.swapxxx.SwapQuoteUniswap
import cash.p.terminal.modules.swapxxx.settings.SwapSettingDeadline
import cash.p.terminal.modules.swapxxx.settings.SwapSettingRecipient
import cash.p.terminal.modules.swapxxx.settings.SwapSettingSlippage
import cash.p.terminal.modules.swapxxx.ui.SwapDataFieldAllowance
import cash.p.terminal.modules.swapxxx.ui.SwapDataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

abstract class BaseUniswapProvider : EvmSwapProvider() {
    private val uniswapKit by lazy { UniswapKit.getInstance() }

    final override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote {
        val blockchainType = tokenIn.blockchainType

        val settingRecipient = SwapSettingRecipient(settings, blockchainType)
        val settingSlippage = SwapSettingSlippage(settings, TradeOptions.defaultAllowedSlippage)
        val settingDeadline = SwapSettingDeadline(settings, TradeOptions.defaultTtl)

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = settingSlippage.valueOrDefault(),
            ttl = settingDeadline.valueOrDefault(),
            recipient = settingRecipient.getEthereumKitAddress(),
        )

        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)
        val swapData = swapDataSingle(tokenIn, tokenOut, evmBlockchainHelper).await()
        val tradeData = uniswapKit.bestTradeExactIn(swapData, amountIn, tradeOptions)
        val routerAddress = uniswapKit.routerAddress(evmBlockchainHelper.chain)
        val allowance = getAllowance(tokenIn, routerAddress)
        val fields = buildList {
            settingSlippage.value?.let {
                add(SwapDataFieldSlippage(it))
            }
            if (allowance != null && allowance < amountIn) {
                add(SwapDataFieldAllowance(allowance, tokenIn))
            }
        }

        return SwapQuoteUniswap(
            tradeData.amountOut!!,
            tradeData.priceImpact,
            fields,
            listOf(settingRecipient, settingSlippage, settingDeadline),
            tokenIn,
            tokenOut,
            amountIn,
            actionApprove(allowance, amountIn, routerAddress, tokenIn)
        )
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
