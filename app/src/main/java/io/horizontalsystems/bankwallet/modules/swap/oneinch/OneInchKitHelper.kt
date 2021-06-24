package io.horizontalsystems.bankwallet.modules.swap.oneinch

import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
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

    private fun getCoinAddress(coin: Coin): Address {
        return when (val coinType = coin.type) {
            CoinType.Ethereum, CoinType.BinanceSmartChain -> evmCoinAddress
            is CoinType.Erc20 -> Address(coinType.address)
            is CoinType.Bep20 -> Address(coinType.address)
            else -> throw IllegalStateException("Unsupported coinType: $coinType")
        }
    }

    val smartContractAddress: Address
        get() = oneInchKit.smartContractAddress

    fun getQuoteAsync(
            fromCoin: Coin,
            toCoin: Coin,
            fromAmount: BigDecimal
    ): Single<Quote> {
        return oneInchKit.getQuoteAsync(
                fromToken = getCoinAddress(fromCoin),
                toToken = getCoinAddress(toCoin),
                amount = fromAmount.scaleUp(fromCoin.decimal)
        )
    }

    fun getSwapAsync(
            fromCoin: Coin,
            toCoin: Coin,
            fromAmount: BigDecimal,
            slippagePercentage: Float,
            recipient: Address? = null,
            gasPrice: Long? = null
    ): Single<Swap> {
        return oneInchKit.getSwapAsync(
                fromToken = getCoinAddress(fromCoin),
                toToken = getCoinAddress(toCoin),
                amount = fromAmount.scaleUp(fromCoin.decimal),
                slippagePercentage = slippagePercentage,
                recipient = recipient,
                gasPrice = gasPrice
        )
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

