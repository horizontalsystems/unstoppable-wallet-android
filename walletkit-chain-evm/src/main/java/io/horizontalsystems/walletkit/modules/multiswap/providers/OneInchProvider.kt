package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.blockTime
import io.horizontalsystems.walletkit.core.convertedError
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.modules.multiswap.EvmBlockchainHelper
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettingsEvm
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.walletkit.scaleUp
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.oneinchkit.OneInchKit
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.toEvmTransactionData

class OneInchProvider(
    // 1inch simulates the sender's allowance/balance server-side before returning
    // calldata; true skips the simulation (e.g. when the approve is batched into
    // the same operation and the allowance is still zero at quote time)
    private val disableEstimate: Boolean? = null,
) : IMultiSwapProvider {
    override val id = ID
    override val title = "1inch"
    override val type = SwapProviderType.DEX
    override val isEvm = true
    override val requireTerms = false
    override val riskLevel = RiskLevel.GOOD
    private val oneInchKit by lazy { OneInchKit.getInstance(App.appConfigProvider.oneInchApiKey) }
    private val partnerAddress: String by lazy { App.appConfigProvider.oneInchPartnerFeeAddress }

    // 1inch's `fee` param is a percentage; SWAP_FEE_BPS is in basis points (100 bps = 1%).
    private val partnerFeePercent: Float by lazy { App.appConfigProvider.swapFeeBps / 100f }

    companion object {
        const val ID = ONEINCH_PROVIDER_ID

        // TODO take evmCoinAddress from oneInchKit
        private val evmCoinAddress = Address("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
    }

    override fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String) = true

    override fun mevProtectionAllowed(tokenIn: Token, tokenOut: Token): Boolean =
        tokenIn.blockchainType == tokenOut.blockchainType && tokenIn.blockchainType.isEvm

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne
            -> true

        else -> false
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal
    ): SwapQuote {
        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val quote = try {
            oneInchKit.getQuoteAsync(
                chain = evmBlockchainHelper.chain,
                fromToken = getTokenAddress(tokenIn),
                toToken = getTokenAddress(tokenOut),
                amount = amountIn.scaleUp(tokenIn.decimals),
                fee = partnerFeePercent
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw e.convertedError
        }

        val routerAddress = OneInchKit.routerAddress(evmBlockchainHelper.chain)
        val allowance = EvmSwapHelper.getAllowance(tokenIn, routerAddress)

        val amountOut = quote.toTokenAmount.toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
        return SwapQuote(
            amountOut = amountOut,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = EvmSwapHelper.actionApprove(allowance, amountIn, routerAddress, tokenIn),
            estimationTime = tokenIn.blockchainType.blockTime
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
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: io.horizontalsystems.walletkit.entities.Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        check(sendTransactionSettings is SendTransactionSettingsEvm)
        if (sendTransactionSettings.gasPriceInfo == null)
            throw OneInchException()

        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val gasPrice = sendTransactionSettings.gasPriceInfo.gasPrice

        val swap = oneInchKit.getSwapAsync(
            chain = evmBlockchainHelper.chain,
            receiveAddress = sendTransactionSettings.receiveAddress,
            fromToken = getTokenAddress(tokenIn),
            toToken = getTokenAddress(tokenOut),
            amount = amountIn.scaleUp(tokenIn.decimals),
            slippagePercentage = slippage.toFloat(),
            recipient = recipient?.hex?.let { Address(it) },
            gasPrice = gasPrice,
            referrer = partnerAddress,
            fee = partnerFeePercent,
            disableEstimate = disableEstimate,
        )

        val swapTx = swap.transaction

        val amountOut = swap.toTokenAmount.toBigDecimal().movePointLeft(swap.toToken.decimals).stripTrailingZeros()
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
            SendTransactionData.Evm(TransactionData(swapTx.to, swapTx.value, swapTx.data).toEvmTransactionData(), swapTx.gasLimit),
            null,
            fields,
            tokenIn.blockchainType.blockTime,
            slippage,
            fromAsset = assetId(tokenIn),
            toAsset = assetId(tokenOut),
        )
    }

    private fun assetId(token: Token): String = when (val type = token.type) {
        is TokenType.Eip20 -> type.address
        else -> evmCoinAddress.hex
    }
}

class OneInchException : RetryableSwapError()