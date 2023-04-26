package io.horizontalsystems.bankwallet.modules.swap.oneinch

import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.modules.swap.scaleUp
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.oneinchkit.Quote
import io.horizontalsystems.oneinchkit.Swap
import io.reactivex.Single
import java.math.BigDecimal

class OneInchKitHelper(
    evmKit: EthereumKit
) {
    private val oneInchKit = OneInchKit.getInstance(evmKit)

    // TODO take evmCoinAddress from oneInchKit
    private val evmCoinAddress = Address("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")

    private fun getTokenAddress(token: Token) = when (val tokenType = token.type) {
        TokenType.Native -> evmCoinAddress
        is TokenType.Eip20 -> Address(tokenType.address)
        else -> throw IllegalStateException("Unsupported tokenType: $tokenType")
    }

    val smartContractAddress: Address
        get() = oneInchKit.routerAddress

    fun getQuoteAsync(
        fromToken: Token,
        toToken: Token,
        fromAmount: BigDecimal
    ): Single<Quote> {
        return oneInchKit.getQuoteAsync(
            fromToken = getTokenAddress(fromToken),
            toToken = getTokenAddress(toToken),
            amount = fromAmount.scaleUp(fromToken.decimals)
        ).onErrorResumeNext {
            Single.error(it.convertedError)
        }
    }

    fun getSwapAsync(
        fromToken: Token,
        toToken: Token,
        fromAmount: BigDecimal,
        slippagePercentage: Float,
        recipient: String? = null,
        gasPrice: GasPrice? = null
    ): Single<Swap> {
        return oneInchKit.getSwapAsync(
            fromToken = getTokenAddress(fromToken),
            toToken = getTokenAddress(toToken),
            amount = fromAmount.scaleUp(fromToken.decimals),
            slippagePercentage = slippagePercentage,
            recipient = recipient?.let { Address(it) },
            gasPrice = gasPrice
        ).onErrorResumeNext {
            Single.error(it.convertedError)
        }
    }

}
