package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.getEthereumKitAddress
import io.horizontalsystems.bankwallet.modules.multiswap.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
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

abstract class BaseUniswapV3Provider(dexType: DexType) : IMultiSwapProvider {
    override val type = SwapProviderType.DEX
    override val aml = true
    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(dexType) }

    final override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal
    ): SwapQuote {
        val bestTrade = fetchBestTrade(tokenIn, tokenOut, amountIn, null, TradeOptions.defaultAllowedSlippage)

        val routerAddress = uniswapV3Kit.routerAddress(bestTrade.chain)
        val allowance = EvmSwapHelper.getAllowance(tokenIn, routerAddress)

        return SwapQuote(
            amountOut = bestTrade.tradeDataV3.tokenAmountOut.decimalAmount!!,
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
        recipient: Address?,
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

        val transactionData = uniswapV3Kit.transactionData(
            sendTransactionSettings.receiveAddress,
            bestTrade.chain,
            bestTrade.tradeDataV3
        )

        val amountOut = bestTrade.tradeDataV3.tokenAmountOut.decimalAmount!!
        val amountOutMin = amountOut - amountOut / BigDecimal(100) * slippage

        val fields = buildList {
            recipient?.let {
                add(DataFieldRecipient(it))
            }
            add(DataFieldSlippage(slippage))
        }

        return SwapFinalQuote(
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
        recipient: Address?,
        slippage: BigDecimal,
    ): UniswapV3BestTrade {
        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)
        val chain = evmBlockchainHelper.chain
        val rpcSourceHttp = evmBlockchainHelper.getRpcSourceHttp()

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = slippage,
            ttl = TradeOptions.defaultTtl,
            recipient = recipient.getEthereumKitAddress(),
        )

        val tradeDataV3 = uniswapV3Kit.bestTradeExactIn(
            rpcSourceHttp,
            chain,
            uniswapToken(tokenIn, chain),
            uniswapToken(tokenOut, chain),
            amountIn,
            tradeOptions,
        )

        return UniswapV3BestTrade(tradeDataV3, chain)
    }

    @Throws
    private fun uniswapToken(token: Token?, chain: Chain) = when (val tokenType = token?.type) {
        TokenType.Native -> when (token.blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
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
    val tradeDataV3: TradeDataV3,
    val chain: Chain
)