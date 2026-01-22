package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.blockTime
import io.horizontalsystems.bankwallet.entities.getEthereumKitAddress
import io.horizontalsystems.bankwallet.modules.multiswap.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
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

abstract class BaseUniswapProvider : IMultiSwapProvider {
    override val type = SwapProviderType.DEX
    override val aml = true
    override val requireTerms = false
    private val uniswapKit by lazy { UniswapKit.getInstance() }

    final override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal
    ): SwapQuote {
        val bestTrade = fetchBestTrade(tokenIn, tokenOut, amountIn, null, TradeOptions.defaultAllowedSlippage)

        val routerAddress = uniswapKit.routerAddress(bestTrade.chain)
        val allowance = EvmSwapHelper.getAllowance(tokenIn, routerAddress)

        return SwapQuote(
            amountOut = bestTrade.tradeData.amountOut!!,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = EvmSwapHelper.actionApprove(allowance, amountIn, routerAddress, tokenIn)
        )
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: io.horizontalsystems.bankwallet.entities.Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        check(sendTransactionSettings is SendTransactionSettings.Evm)

        val bestTrade = fetchBestTrade(
            tokenIn,
            tokenOut,
            amountIn,
            recipient,
            slippage
        )

        val transactionData = uniswapKit.transactionData(
            sendTransactionSettings.receiveAddress,
            bestTrade.chain,
            bestTrade.tradeData
        )

        val amountOut = bestTrade.tradeData.amountOut!!
        val amountOutMin = amountOut - amountOut / BigDecimal(100) * slippage

        val fields = buildList {
            recipient?.let {
                add(DataFieldRecipient(it))
            }
            DataFieldSlippage.getField(slippage)?.let {
                add(it)
            }
        }

        return SwapFinalQuote(
            tokenIn,
            tokenOut,
            amountIn,
            amountOut,
            amountOutMin,
            SendTransactionData.Evm(transactionData, null),
            bestTrade.tradeData.priceImpact,
            fields,
            tokenIn.blockchainType.blockTime,
            slippage
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
        recipient: io.horizontalsystems.bankwallet.entities.Address?,
        slippage: BigDecimal,
    ): UniswapBestTrade {
        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)
        val chain = evmBlockchainHelper.chain
        val rpcSourceHttp = evmBlockchainHelper.getRpcSourceHttp()

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = slippage,
            ttl = TradeOptions.defaultTtl,
            recipient = recipient.getEthereumKitAddress(),
        )

        val swapData = uniswapKit.swapData(
            rpcSourceHttp,
            chain,
            uniswapToken(tokenIn, chain),
            uniswapToken(tokenOut, chain)
        ).await()

        val tradeData = uniswapKit.bestTradeExactIn(swapData, amountIn, tradeOptions)

        return UniswapBestTrade(tradeData, chain)
    }
}

private data class UniswapBestTrade(
    val tradeData: TradeData,
    val chain: Chain
)
