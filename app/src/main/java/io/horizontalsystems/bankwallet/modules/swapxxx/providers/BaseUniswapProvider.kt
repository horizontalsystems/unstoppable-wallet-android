package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.modules.swapxxx.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.swapxxx.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swapxxx.SwapQuoteUniswap
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingFieldDeadline
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingFieldRecipient
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingFieldSlippage
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataField
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapFeeField
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

abstract class BaseUniswapProvider : ISwapXxxProvider {
    private val uniswapKit by lazy { UniswapKit.getInstance() }

    final override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote {
        val blockchainType = tokenIn.blockchainType

        val fieldRecipient = SwapSettingFieldRecipient(settings, blockchainType)
        val fieldSlippage = SwapSettingFieldSlippage(settings, TradeOptions.defaultAllowedSlippage)
        val fieldDeadline = SwapSettingFieldDeadline(settings, TradeOptions.defaultTtl)

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = fieldSlippage.value ?: TradeOptions.defaultAllowedSlippage,
            ttl = fieldDeadline.value ?: TradeOptions.defaultTtl,
            recipient = fieldRecipient.getEthereumKitAddress(),
        )

        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)
        val swapData = swapDataSingle(tokenIn, tokenOut, evmBlockchainHelper).await()
        val tradeData = uniswapKit.bestTradeExactIn(swapData, amountIn, tradeOptions)
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

        return SwapQuoteUniswap(
            tradeData.amountOut!!,
            fields,
            feeAmountData,
            listOf(fieldRecipient, fieldSlippage, fieldDeadline)
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
