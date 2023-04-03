package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

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
    private val lowPriorityBlockCount = 6
    private val mediumPriorityBlockCount = 2
    private val highPriorityBlockCount = 1

    override fun getFeeRateRange() = 1..200.toLong() // the max fee rate 200 has been chosen in the result of research

    override val feeRatePriorityList: List<FeeRatePriority> = listOf(
        FeeRatePriority.LOW,
        FeeRatePriority.RECOMMENDED,
        FeeRatePriority.HIGH,
        FeeRatePriority.Custom(0),
    )

    override suspend fun getFeeRate(feeRatePriority: FeeRatePriority) = when (feeRatePriority) {
        is FeeRatePriority.Custom -> feeRatePriority.value
        FeeRatePriority.LOW -> fetchFeeRate(lowPriorityBlockCount)
        FeeRatePriority.RECOMMENDED -> fetchFeeRate(mediumPriorityBlockCount)
        FeeRatePriority.HIGH -> fetchFeeRate(highPriorityBlockCount)
    }

    private suspend fun fetchFeeRate(blockCount: Int) = withContext(Dispatchers.IO) {
        feeRateProvider.bitcoinFeeRate(blockCount).blockingGet().toLong()
    }
}

class LitecoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRate(feeRatePriority: FeeRatePriority) = withContext(Dispatchers.IO) {
        feeRateProvider.litecoinFeeRate().blockingGet().toLong()
    }
}

class BitcoinCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRate(feeRatePriority: FeeRatePriority) = withContext(Dispatchers.IO) {
        feeRateProvider.bitcoinCashFeeRate().blockingGet().toLong()
    }
}

class DashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRate(feeRatePriority: FeeRatePriority) = withContext(Dispatchers.IO) {
        feeRateProvider.dashFeeRate().blockingGet().toLong()
    }
}

class ECashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRate(feeRatePriority: FeeRatePriority) = withContext(Dispatchers.IO) {
        1L
    }
}
