package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.modules.multiswap.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuoteEvm
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuoteUniswap
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingDeadline
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldAllowance
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipientExtended
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
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
        val bestTrade = fetchBestTrade(tokenIn, tokenOut, amountIn, settings)

        val routerAddress = uniswapKit.routerAddress(bestTrade.chain)
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

        return SwapQuoteUniswap(
            bestTrade.tradeData,
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

        val transactionData = uniswapKit.transactionData(
            sendTransactionSettings.receiveAddress,
            bestTrade.chain,
            bestTrade.tradeData
        )

        val slippage = bestTrade.settingSlippage.valueOrDefault()
        val amountOut = bestTrade.tradeData.amountOut!!
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
            bestTrade.tradeData.priceImpact,
            fields
        )
    }

    @Throws
    private fun uniswapToken(token: Token?, chain: Chain) = when (val tokenType = token?.type) {
        TokenType.Native -> uniswapKit.etherToken(chain)
        is TokenType.Eip20 -> {
            uniswapKit.token(Address(tokenType.address), token.decimals)
        }

        else -> throw Exception("Invalid coin for swap: $token")
    }

    private suspend fun fetchBestTrade(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): UniswapBestTrade {
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

        val swapData = uniswapKit.swapData(
            rpcSourceHttp,
            chain,
            uniswapToken(tokenIn, chain),
            uniswapToken(tokenOut, chain)
        ).await()

        val tradeData = uniswapKit.bestTradeExactIn(swapData, amountIn, tradeOptions)

        return UniswapBestTrade(
            settingRecipient,
            settingSlippage,
            settingDeadline,
            tradeData,
            chain
        )
    }
}

private data class UniswapBestTrade(
    val settingRecipient: SwapSettingRecipient,
    val settingSlippage: SwapSettingSlippage,
    val settingDeadline: SwapSettingDeadline,
    val tradeData: TradeData,
    val chain: Chain
)
