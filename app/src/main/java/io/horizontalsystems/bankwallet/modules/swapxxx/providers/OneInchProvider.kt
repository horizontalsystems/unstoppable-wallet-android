package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.modules.swap.scaleUp
import io.horizontalsystems.bankwallet.modules.swapxxx.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.swapxxx.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swapxxx.SwapQuoteOneInch
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.oneinchkit.OneInchKit
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

object OneInchProvider : ISwapXxxProvider {
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

        val amountOut = quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
        val fields = buildList {
            settingSlippage.value?.let {
                add(SwapDataFieldSlippage(it))
            }
        }

        return SwapQuoteOneInch(
            amountOut,
            null,
            fields,
            null,
            listOf(settingRecipient, settingSlippage)
        )
    }

    private fun getTokenAddress(token: Token) = when (val tokenType = token.type) {
        TokenType.Native -> evmCoinAddress
        is TokenType.Eip20 -> Address(tokenType.address)
        else -> throw IllegalStateException("Unsupported tokenType: $tokenType")
    }
}
