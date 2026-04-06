package cash.p.terminal.core.providers

import cash.p.terminal.core.App
import cash.p.terminal.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.horizontalsystems.feeratekit.providers.BitcoinFeeProvider
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigInteger

class FeeRateProvider {

    private val feeRateKit: FeeRateKit by lazy {
        FeeRateKit(
            FeeProviderConfig(
                ethEvmUrl = AppConfigProvider.blocksDecodedEthereumRpc,
                ethEvmAuth = null,
                bscEvmUrl = FeeProviderConfig.defaultBscEvmUrl(),
                mempoolSpaceUrl = AppConfigProvider.mempoolSpaceUrl,
                blockCypherUrl = AppConfigProvider.blockCypherUrl,
                torEnabled = App.torKitManager.isTorEnabled
            )
        )
    }

    suspend fun bitcoinFeeRate(): BitcoinFeeProvider.RecommendedFees {
        return feeRateKit.bitcoin()
    }

    suspend fun litecoinFeeRate(): BigInteger {
        return feeRateKit.litecoin()
    }

    fun dogecoinFeeRate(): Single<BigInteger> {
        return Single.just(BigInteger("51000"))
    }

    suspend fun bitcoinCashFeeRate(): BigInteger {
        return feeRateKit.bitcoinCash()
    }

    suspend fun dashFeeRate(): BigInteger {
        return feeRateKit.dash()
    }

    fun pirateCashFeeRate(): Single<BigInteger> {
        return Single.just(BigInteger("7000"))
    }

}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override val feeRateChangeable = true

    override suspend fun getFeeRates(): FeeRates {
        val bitcoinFeeRate = feeRateProvider.bitcoinFeeRate()
        return FeeRates(bitcoinFeeRate.halfHourFee, bitcoinFeeRate.minimumFee)
    }
}

class LitecoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        return FeeRates(2)
    }
}

class DogecoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.dogecoinFeeRate().await()
        return FeeRates(feeRate.toInt())
    }
}

class BitcoinCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.bitcoinCashFeeRate()
        return FeeRates(feeRate.toInt())
    }
}

class DashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.dashFeeRate()
        return FeeRates(feeRate.toInt())
    }
}

class CosantaFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.dashFeeRate()
        return FeeRates(feeRate.toInt())
    }
}

class PirateCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.pirateCashFeeRate().await()
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