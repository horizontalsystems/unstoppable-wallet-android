package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.convertedError
import cash.p.terminal.modules.swap.scaleUp
import cash.p.terminal.modules.swapxxx.EvmBlockchainHelper
import cash.p.terminal.modules.swapxxx.ISwapQuote
import cash.p.terminal.modules.swapxxx.SwapQuoteOneInch
import cash.p.terminal.modules.swapxxx.sendtransaction.SendTransactionData
import cash.p.terminal.modules.swapxxx.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.swapxxx.settings.SwapSettingRecipient
import cash.p.terminal.modules.swapxxx.settings.SwapSettingSlippage
import cash.p.terminal.modules.swapxxx.ui.SwapDataFieldAllowance
import cash.p.terminal.modules.swapxxx.ui.SwapDataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.oneinchkit.OneInchKit
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

object OneInchProvider : EvmSwapProvider() {
    override val id = "oneinch"
    override val title = "1inch"
    override val url = "https://app.1inch.io/"
    override val icon = R.drawable.oneinch
    private val oneInchKit by lazy { OneInchKit.getInstance(App.appConfigProvider.oneInchApiKey) }

    // TODO take evmCoinAddress from oneInchKit
    private val evmCoinAddress = Address("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne
        -> true

        else -> false
    }

    override suspend fun getSendTransactionData(
        swapQuote: ISwapQuote,
        sendTransactionSettings: SendTransactionSettings?,
        swapSettings: Map<String, Any?>
    ): SendTransactionData {
        check(sendTransactionSettings is SendTransactionSettings.Evm)

        val blockchainType = swapQuote.tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val gasPrice = sendTransactionSettings.gasPriceInfo?.gasPrice

        val evmKitWrapper = evmBlockchainHelper.evmKitWrapper ?: throw NullPointerException()

        val settingRecipient = SwapSettingRecipient(swapSettings, blockchainType)
        val settingSlippage = SwapSettingSlippage(swapSettings, BigDecimal("1"))


        val swap = oneInchKit.getSwapAsync(
            chain = evmBlockchainHelper.chain,
            receiveAddress = evmKitWrapper.evmKit.receiveAddress,
            fromToken = getTokenAddress(swapQuote.tokenIn),
            toToken = getTokenAddress(swapQuote.tokenOut),
            amount = swapQuote.amountIn.scaleUp(swapQuote.tokenIn.decimals),
            slippagePercentage = settingSlippage.valueOrDefault().toFloat(),
            recipient = settingRecipient.value?.hex?.let { Address(it) },
            gasPrice = gasPrice
        ).await()

        val swapTx = swap.transaction

        return SendTransactionData.Evm(TransactionData(swapTx.to, swapTx.value, swapTx.data))
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote {
        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val settingRecipient = SwapSettingRecipient(settings, blockchainType)
        val settingSlippage = SwapSettingSlippage(settings, BigDecimal("1"))

        val quote = oneInchKit.getQuoteAsync(
            chain = evmBlockchainHelper.chain,
            fromToken = getTokenAddress(tokenIn),
            toToken = getTokenAddress(tokenOut),
            amount = amountIn.scaleUp(tokenIn.decimals)
        ).onErrorResumeNext {
            Single.error(it.convertedError)
        }.await()

        val amountOut = quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
        val fields = buildList {
            settingSlippage.value?.let {
                add(SwapDataFieldSlippage(it))
            }
            getAllowance(tokenIn, OneInchKit.routerAddress(evmBlockchainHelper.chain))?.let {
                add(SwapDataFieldAllowance(it, tokenIn))
            }
        }

        return SwapQuoteOneInch(
            amountOut,
            null,
            fields,
            listOf(settingRecipient, settingSlippage),
            tokenIn,
            tokenOut,
            amountIn
        )
    }

    private fun getTokenAddress(token: Token) = when (val tokenType = token.type) {
        TokenType.Native -> evmCoinAddress
        is TokenType.Eip20 -> Address(tokenType.address)
        else -> throw IllegalStateException("Unsupported tokenType: $tokenType")
    }
}
