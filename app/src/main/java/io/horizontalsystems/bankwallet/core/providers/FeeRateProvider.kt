package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.horizontalsystems.feeratekit.providers.MempoolSpaceProvider
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigInteger

class FeeRateProvider(appConfig: AppConfigProvider) {

    private val feeRateKit: FeeRateKit by lazy {
        FeeRateKit(
            FeeProviderConfig(
                ethEvmUrl = appConfig.blocksDecodedEthereumRpc,
                ethEvmAuth = null,
                bscEvmUrl = FeeProviderConfig.defaultBscEvmUrl(),
                mempoolSpaceUrl = appConfig.mempoolSpaceUrl
            )
        )
    }

    fun bitcoinFeeRate(): Single<MempoolSpaceProvider.RecommendedFees> {
        return feeRateKit.bitcoin()
    }

    fun litecoinFeeRate(): Single<BigInteger> {
        return feeRateKit.litecoin()
    }

    fun bitcoinCashFeeRate(): Single<BigInteger> {
        return feeRateKit.bitcoinCash()
    }

    fun dashFeeRate(): Single<BigInteger> {
        return feeRateKit.dash()
    }

}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override val feeRateChangeable = true

    override suspend fun getFeeRates(): FeeRates {
        val bitcoinFeeRate = feeRateProvider.bitcoinFeeRate().await()
        return FeeRates(bitcoinFeeRate.halfHourFee, bitcoinFeeRate.minimumFee)
    }
}

class LitecoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        return FeeRates(2)
    }
}

class BitcoinCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.bitcoinCashFeeRate().await()
        return FeeRates(feeRate.toInt())
    }
}

class DashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.dashFeeRate().await()
        return FeeRates(feeRate.toInt())
    }
}

class ECashFeeRateProvider : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        return FeeRates(2)
    }
}

data class FeeRates(
    val recommended: Int,
    val minimum: Int = 0,
)