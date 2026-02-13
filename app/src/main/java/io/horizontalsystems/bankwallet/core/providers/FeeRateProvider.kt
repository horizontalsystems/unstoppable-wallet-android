package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.horizontalsystems.feeratekit.providers.BitcoinFeeProvider
import java.math.BigInteger

class FeeRateProvider(appConfig: AppConfigProvider) {

    private val feeRateKit: FeeRateKit by lazy {
        FeeRateKit(
            FeeProviderConfig(
                ethEvmUrl = appConfig.blocksDecodedEthereumRpc,
                ethEvmAuth = null,
                bscEvmUrl = FeeProviderConfig.defaultBscEvmUrl(),
                mempoolSpaceUrl = appConfig.mempoolSpaceUrl,
                blockCypherUrl = appConfig.blockCypherUrl,
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

    suspend fun bitcoinCashFeeRate(): BigInteger {
        return feeRateKit.bitcoinCash()
    }

    suspend fun dashFeeRate(): BigInteger {
        return feeRateKit.dash()
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
        val feeRate = feeRateProvider.litecoinFeeRate()
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

class ECashFeeRateProvider : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        return FeeRates(2)
    }
}

data class FeeRates(
    val recommended: Int,
    val minimum: Int = 0,
)