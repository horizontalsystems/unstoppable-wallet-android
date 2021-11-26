package io.horizontalsystems.bankwallet.modules.swap.oneinch

import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.oneinchkit.Quote
import io.horizontalsystems.oneinchkit.Swap
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.absoluteValue

class OneInchKitHelper(
    evmKit: EthereumKit
) {
    private val oneInchKit = OneInchKit.getInstance(evmKit)

    // TODO take evmCoinAddress from oneInchKit
    private val evmCoinAddress = Address("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")

    private fun getCoinAddress(coin: PlatformCoin): Address {
        return when (val coinType = coin.coinType) {
            CoinType.Ethereum, CoinType.BinanceSmartChain -> evmCoinAddress
            is CoinType.Erc20 -> Address(coinType.address)
            is CoinType.Bep20 -> Address(coinType.address)
            else -> throw IllegalStateException("Unsupported coinType: $coinType")
        }
    }

    val smartContractAddress: Address
        get() = oneInchKit.smartContractAddress

    fun getQuoteAsync(
        fromCoin: PlatformCoin,
        toCoin: PlatformCoin,
        fromAmount: BigDecimal
    ): Single<Quote> {
        return oneInchKit.getQuoteAsync(
            fromToken = getCoinAddress(fromCoin),
            toToken = getCoinAddress(toCoin),
            amount = fromAmount.scaleUp(fromCoin.decimals)
        ).onErrorResumeNext {
            Single.error(it.convertedError)
        }
    }

    fun getSwapAsync(
        fromCoin: PlatformCoin,
        toCoin: PlatformCoin,
        fromAmount: BigDecimal,
        slippagePercentage: Float,
        recipient: String? = null,
        gasPrice: Long? = null
    ): Single<Swap> {
        return oneInchKit.getSwapAsync(
            fromToken = getCoinAddress(fromCoin),
            toToken = getCoinAddress(toCoin),
            amount = fromAmount.scaleUp(fromCoin.decimals),
            slippagePercentage = slippagePercentage,
            recipient = recipient?.let { Address(it) },
            gasPrice = gasPrice
        ).onErrorResumeNext {
            Single.error(it.convertedError)
        }
    }

}

fun BigDecimal.scaleUp(scale: Int): BigInteger {
    val exponent = scale - scale()

    return if (exponent >= 0) {
        unscaledValue() * BigInteger.TEN.pow(exponent)
    } else {
        unscaledValue() / BigInteger.TEN.pow(exponent.absoluteValue)
    }
}

