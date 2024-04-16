package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.modules.multiswap.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuoteEvm
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuoteUniswapV3
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingDeadline
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldAllowance
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipientExtended
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import java.math.BigDecimal

abstract class BaseUniswapV3Provider(dexType: DexType) : EvmSwapProvider() {
    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(dexType) }

    final override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote {
        val bestTrade = fetchBestTrade(tokenIn, tokenOut, amountIn, settings)

        val routerAddress = uniswapV3Kit.routerAddress(bestTrade.chain)
        val allowance = getAllowance(tokenIn, routerAddress)

        val fields = buildList {
            bestTrade.settingRecipient.value?.let {
                add(DataFieldRecipient(it))
            }
            bestTrade.settingSlippage.value?.let {
                add(DataFieldSlippage(it))
            }
            if (allowance != null && allowance < amountIn) {
                add(DataFieldAllowance(allowance, tokenIn))
            }
        }

        return SwapQuoteUniswapV3(
            bestTrade.tradeDataV3,
            fields,
            listOf(bestTrade.settingRecipient, bestTrade.settingSlippage, bestTrade.settingDeadline),
            tokenIn,
            tokenOut,
            amountIn,
            actionApprove(allowance, amountIn, routerAddress, tokenIn)
        )
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
    ): ISwapFinalQuote {
        check(sendTransactionSettings is SendTransactionSettings.Evm)

        val bestTrade = fetchBestTrade(
            tokenIn,
            tokenOut,
            amountIn,
            swapSettings
        )

        val transactionData = uniswapV3Kit.transactionData(
            sendTransactionSettings.receiveAddress,
            bestTrade.chain,
            bestTrade.tradeDataV3
        )

        val slippage = bestTrade.settingSlippage.valueOrDefault()
        val amountOut = bestTrade.tradeDataV3.tokenAmountOut.decimalAmount!!
        val amountOutMin = amountOut - amountOut / BigDecimal(100) * slippage

        val fields = buildList {
            bestTrade.settingRecipient.value?.let {
                add(DataFieldRecipientExtended(it, tokenOut.blockchainType))
            }
            bestTrade.settingSlippage.value?.let {
                add(DataFieldSlippage(it))
            }
        }

        return SwapFinalQuoteEvm(
            tokenIn,
            tokenOut,
            amountIn,
            amountOut,
            amountOutMin,
            SendTransactionData.Evm(transactionData, null),
            bestTrade.tradeDataV3.priceImpact,
            fields
        )
    }

    private suspend fun fetchBestTrade(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): UniswapV3BestTrade {
        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)
        val chain = evmBlockchainHelper.chain
        val rpcSourceHttp = evmBlockchainHelper.getRpcSourceHttp()

        val settingRecipient = SwapSettingRecipient(settings, blockchainType)
        val settingSlippage = SwapSettingSlippage(settings, TradeOptions.defaultAllowedSlippage)
        val settingDeadline = SwapSettingDeadline(settings, TradeOptions.defaultTtl)

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = settingSlippage.valueOrDefault(),
            ttl = settingDeadline.valueOrDefault(),
            recipient = settingRecipient.getEthereumKitAddress(),
        )

        val tradeDataV3 = uniswapV3Kit.bestTradeExactIn(
            rpcSourceHttp,
            chain,
            uniswapToken(tokenIn, chain),
            uniswapToken(tokenOut, chain),
            amountIn,
            tradeOptions,
        )

        return UniswapV3BestTrade(
            settingRecipient,
            settingSlippage,
            settingDeadline,
            tradeDataV3,
            chain
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

private data class UniswapV3BestTrade(
    val settingRecipient: SwapSettingRecipient,
    val settingSlippage: SwapSettingSlippage,
    val settingDeadline: SwapSettingDeadline,
    val tradeDataV3: TradeDataV3,
    val chain: Chain
)