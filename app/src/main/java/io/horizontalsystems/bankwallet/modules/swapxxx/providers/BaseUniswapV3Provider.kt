package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.modules.swapxxx.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.swapxxx.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swapxxx.SwapQuoteUniswapV3
import io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingDeadline
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataFieldAllowance
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.TradeOptions
import kotlinx.coroutines.delay
import java.math.BigDecimal

abstract class BaseUniswapV3Provider(dexType: DexType) : EvmSwapProvider() {
    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(dexType) }

    override suspend fun getSendTransactionData(
        swapQuote: ISwapQuote,
        sendTransactionSettings: SendTransactionSettings?,
        swapSettings: Map<String, Any?>
    ): SendTransactionData {
        check(swapQuote is SwapQuoteUniswapV3)

        val blockchainType = swapQuote.tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val transactionData = evmBlockchainHelper.receiveAddress?.let { receiveAddress ->
            uniswapV3Kit.transactionData(receiveAddress, evmBlockchainHelper.chain, swapQuote.tradeDataV3)
        } ?: throw Exception("Yahoo")

        return SendTransactionData.Evm(transactionData)
    }

    override suspend fun swap(swapQuote: ISwapQuote) {
        check(swapQuote is SwapQuoteUniswapV3)

        val blockchainType = swapQuote.tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)
        val evmKitWrapper = evmBlockchainHelper.evmKitWrapper ?: return

        val transactionData = evmBlockchainHelper.receiveAddress?.let { receiveAddress ->
            uniswapV3Kit.transactionData(receiveAddress, evmBlockchainHelper.chain, swapQuote.tradeDataV3)
        } ?: throw Exception("Yahoo")

        val feeData = evmBlockchainHelper.getFeeData(transactionData) ?: throw Exception("Yahoo")

        val gasLimit = feeData.gasData.gasLimit
        val gasPrice = feeData.gasData.gasPrice
        val nonce: Long? = null

        try {
            delay(1000)
//            val transaction = evmKitWrapper.sendSingle(transactionData, gasPrice, gasLimit, nonce).await()
//            logger.info("success")
        } catch (e: Throwable) {
//            logger.warning("failed", error)
        }
    }

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

        val fields = buildList {
            settingSlippage.value?.let {
                add(SwapDataFieldSlippage(it))
            }
            getAllowance(tokenIn, uniswapV3Kit.routerAddress(chain))?.let {
                add(SwapDataFieldAllowance(it, tokenIn))
            }
        }

        return SwapQuoteUniswapV3(
            tradeDataV3,
            fields,
            listOf(settingRecipient, settingSlippage, settingDeadline),
            tokenIn,
            tokenOut,
            amountIn
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
