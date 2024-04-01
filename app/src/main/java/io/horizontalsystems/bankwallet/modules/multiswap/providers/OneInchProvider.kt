package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.modules.multiswap.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuoteEvm
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuoteOneInch
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldAllowance
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipientExtended
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.bankwallet.modules.swap.scaleUp
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

        val routerAddress = OneInchKit.routerAddress(evmBlockchainHelper.chain)
        val allowance = getAllowance(tokenIn, routerAddress)
        val fields = buildList {
            settingRecipient.value?.let {
                add(DataFieldRecipient(it))
            }
            settingSlippage.value?.let {
                add(DataFieldSlippage(it))
            }
            if (allowance != null && allowance < amountIn) {
                add(DataFieldAllowance(allowance, tokenIn))
            }
        }

        val amountOut = quote.toTokenAmount.toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
        return SwapQuoteOneInch(
            amountOut,
            null,
            fields,
            listOf(settingRecipient, settingSlippage),
            tokenIn,
            tokenOut,
            amountIn,
            actionApprove(allowance, amountIn, routerAddress, tokenIn)
        )
    }

    private fun getTokenAddress(token: Token) = when (val tokenType = token.type) {
        TokenType.Native -> evmCoinAddress
        is TokenType.Eip20 -> Address(tokenType.address)
        else -> throw IllegalStateException("Unsupported tokenType: $tokenType")
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
    ): ISwapFinalQuote {
        check(sendTransactionSettings is SendTransactionSettings.Evm)
        checkNotNull(sendTransactionSettings.gasPriceInfo)

        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val gasPrice = sendTransactionSettings.gasPriceInfo.gasPrice

        val settingRecipient = SwapSettingRecipient(swapSettings, blockchainType)
        val settingSlippage = SwapSettingSlippage(swapSettings, BigDecimal("1"))
        val slippage = settingSlippage.valueOrDefault()

        val swap = oneInchKit.getSwapAsync(
            chain = evmBlockchainHelper.chain,
            receiveAddress = sendTransactionSettings.receiveAddress,
            fromToken = getTokenAddress(tokenIn),
            toToken = getTokenAddress(tokenOut),
            amount = amountIn.scaleUp(tokenIn.decimals),
            slippagePercentage = slippage.toFloat(),
            recipient = settingRecipient.value?.hex?.let { Address(it) },
            gasPrice = gasPrice
        ).await()

        val swapTx = swap.transaction

        val amountOut = swap.toTokenAmount.toBigDecimal().movePointLeft(swap.toToken.decimals).stripTrailingZeros()
        val amountOutMin = amountOut - amountOut / BigDecimal(100) * slippage

        val fields = buildList {
            settingRecipient.value?.let {
                add(DataFieldRecipientExtended(it, tokenOut.blockchainType))
            }
            settingSlippage.value?.let {
                add(DataFieldSlippage(it))
            }
        }

        return SwapFinalQuoteEvm(
            tokenIn,
            tokenOut,
            amountIn,
            amountOut,
            amountOutMin,
            SendTransactionData.Evm(TransactionData(swapTx.to, swapTx.value, swapTx.data), swapTx.gasLimit),
            null,
            fields
        )
    }
}
