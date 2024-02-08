package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.modules.swapxxx.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.swapxxx.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swapxxx.SwapQuoteUniswapV3
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingDeadline
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataFieldFee
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.TradeOptions
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

        val settingRecipient = SwapSettingRecipient(settings, blockchainType)
        val settingSlippage = SwapSettingSlippage(settings, TradeOptions.defaultAllowedSlippage)
        val settingDeadline = SwapSettingDeadline(settings, TradeOptions.defaultTtl)

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = settingSlippage.valueOrDefault(),
            ttl = settingDeadline.valueOrDefault(),
            recipient = settingRecipient.getEthereumKitAddress(),
        )

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
            tradeOptions
        )
        val amountOut = tradeDataV3.tokenAmountOut.decimalAmount!!

        val transactionData = evmBlockchainHelper.receiveAddress?.let { receiveAddress ->
            uniswapV3Kit.transactionData(receiveAddress, chain, tradeDataV3)
        }
        val feeAmountData = transactionData?.let {
            evmBlockchainHelper.getFeeAmountData(it)
        }

        val fields = buildList {
            feeAmountData?.let {
                add(SwapDataFieldFee(it))
            }
            settingSlippage.value?.let {
                add(SwapDataFieldSlippage(it))
            }
        }

        return SwapQuoteUniswapV3(
            amountOut,
            tradeDataV3.priceImpact,
            fields,
            feeAmountData,
            listOf(settingRecipient, settingSlippage, settingDeadline)
        )
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
