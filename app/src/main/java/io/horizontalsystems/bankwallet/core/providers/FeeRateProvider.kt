package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.reactivex.Single
import java.math.BigInteger
import kotlin.math.ceil

class FeeRateProvider(appConfig: AppConfigProvider) {

    private val feeRateKit: FeeRateKit by lazy {
        FeeRateKit(FeeProviderConfig(
                ethEvmUrl = FeeProviderConfig.infuraUrl(appConfig.infuraProjectId),
                ethEvmAuth = appConfig.infuraProjectSecret,
                bscEvmUrl = FeeProviderConfig.defaultBscEvmUrl(),
                btcCoreRpcUrl = appConfig.btcCoreRpcUrl
            )
        )
    }

    fun bitcoinFeeRate(blockCount: Int): Single<BigInteger> {
        return feeRateKit.bitcoin(blockCount)
    }

    fun binanceSmartChainGasPrice(): Single<BigInteger> {
        return feeRateKit.binanceSmartChain()
    }

    fun litecoinFeeRate(): Single<BigInteger> {
        return feeRateKit.litecoin()
    }

    fun bitcoinCashFeeRate(): Single<BigInteger> {
        return feeRateKit.bitcoinCash()
    }

    fun ethereumGasPrice(): Single<BigInteger> {
        return feeRateKit.ethereum()
    }

    fun dashFeeRate(): Single<BigInteger> {
        return feeRateKit.dash()
    }

}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {

    private val lowPriorityBlockCount = 40
    private val mediumPriorityBlockCount = 8
    private val highPriorityBlockCount = 2

    override val feeRatePriorityList: List<FeeRatePriority> = listOf(
            FeeRatePriority.LOW,
            FeeRatePriority.RECOMMENDED,
            FeeRatePriority.HIGH,
            FeeRatePriority.Custom(1, LongRange(1, 200))
    )

    override val recommendedFeeRate: Single<BigInteger> = feeRateProvider.bitcoinFeeRate(mediumPriorityBlockCount)

    override var defaultFeeRatePriority: FeeRatePriority = FeeRatePriority.RECOMMENDED

    override fun feeRate(feeRatePriority: FeeRatePriority): Single<BigInteger> {
        return when (feeRatePriority) {
            FeeRatePriority.LOW -> feeRateProvider.bitcoinFeeRate(lowPriorityBlockCount)
            FeeRatePriority.HIGH -> feeRateProvider.bitcoinFeeRate(highPriorityBlockCount)
            FeeRatePriority.RECOMMENDED -> feeRateProvider.bitcoinFeeRate(mediumPriorityBlockCount)
            is FeeRatePriority.Custom -> Single.just(feeRatePriority.value.toBigInteger())
        }
    }
}

class LitecoinFeeRateProvider(feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override val feeRatePriorityList: List<FeeRatePriority> = listOf()
    override val recommendedFeeRate: Single<BigInteger> = feeRateProvider.litecoinFeeRate()
}

class BitcoinCashFeeRateProvider(feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override val feeRatePriorityList: List<FeeRatePriority> = listOf()
    override val recommendedFeeRate: Single<BigInteger> = feeRateProvider.bitcoinCashFeeRate()
}

class EthereumFeeRateProvider(
    feeRateProvider: FeeRateProvider,
    feeLowerBound: Long? = null,
    feeUpperBound: Long? = null,
    multiply: Double? = null
) : ICustomRangedFeeProvider {

    override val customFeeRange = LongRange(feeLowerBound ?: defaultLowerBound, feeUpperBound ?: defaultUpperBound)

    override val feeRatePriorityList: List<FeeRatePriority> = listOf(
        FeeRatePriority.RECOMMENDED,
        FeeRatePriority.Custom(1, customFeeRange)
    )

    override val recommendedFeeRate: Single<BigInteger> =
        feeRateProvider.ethereumGasPrice()
            .map { getAdjustedGasPrice(maxOf(it.toLong(), customFeeRange.first), multiply) }

    companion object {
        private const val defaultLowerBound: Long = 1_000_000_000
        private const val defaultUpperBound: Long = 400_000_000_000
    }
}

class BinanceSmartChainFeeRateProvider(
    feeRateProvider: FeeRateProvider,
    feeLowerBound: Long? = null,
    feeUpperBound: Long? = null,
    multiply: Double? = null
) : ICustomRangedFeeProvider {

    override val customFeeRange = LongRange(feeLowerBound ?: defaultLowerBound, feeUpperBound ?: defaultUpperBound)

    override val feeRatePriorityList: List<FeeRatePriority> = listOf(
        FeeRatePriority.RECOMMENDED,
        FeeRatePriority.Custom(1, customFeeRange)
    )

    override val recommendedFeeRate: Single<BigInteger> =
        feeRateProvider.ethereumGasPrice()
            .map { getAdjustedGasPrice(maxOf(it.toLong(), customFeeRange.first), multiply) }

    companion object {
        private const val defaultLowerBound: Long = 1_000_000_000
        private const val defaultUpperBound: Long = 400_000_000_000
    }
}

class DashFeeRateProvider(feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override val feeRatePriorityList: List<FeeRatePriority> = listOf()
    override val recommendedFeeRate: Single<BigInteger> = feeRateProvider.dashFeeRate()
}

private fun getAdjustedGasPrice(recommendedGasPrice: Long, multiply: Double?): BigInteger {
    val adjustedGasPrice = recommendedGasPrice.toDouble() * (multiply ?: 1.0)
    return ceil(adjustedGasPrice).toBigDecimal().toBigInteger()
}
