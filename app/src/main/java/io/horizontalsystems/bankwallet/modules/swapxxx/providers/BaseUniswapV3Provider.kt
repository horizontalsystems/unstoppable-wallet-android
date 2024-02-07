package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.modules.swapxxx.EvmBlockchainHelper
import cash.p.terminal.modules.swapxxx.ISwapQuote
import cash.p.terminal.modules.swapxxx.SwapQuoteUniswapV3
import cash.p.terminal.modules.swapxxx.settings.SwapSettingFieldDeadline
import cash.p.terminal.modules.swapxxx.settings.SwapSettingFieldRecipient
import cash.p.terminal.modules.swapxxx.settings.SwapSettingFieldSlippage
import cash.p.terminal.modules.swapxxx.ui.SwapDataField
import cash.p.terminal.modules.swapxxx.ui.SwapFeeField
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

        val fieldRecipient = SwapSettingFieldRecipient(settings, blockchainType)
        val fieldSlippage = SwapSettingFieldSlippage(settings, TradeOptions.defaultAllowedSlippage)
        val fieldDeadline = SwapSettingFieldDeadline(settings, TradeOptions.defaultTtl)

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = fieldSlippage.valueOrDefault(),
            ttl = fieldDeadline.valueOrDefault(),
            recipient = fieldRecipient.getEthereumKitAddress(),
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
            evmBlockchainHelper.getFeeAmountData(transactionData)
        }

        val fields = buildList<SwapDataField> {
            feeAmountData?.let {
                add(SwapFeeField(feeAmountData))
            }
        }

        return SwapQuoteUniswapV3(
            amountOut,
            tradeDataV3.priceImpact,
            fields,
            feeAmountData,
            listOf(fieldRecipient, fieldSlippage, fieldDeadline)
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
