package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.horizontalsystems.feeratekit.model.FeeRate
import io.reactivex.Single

class FeeRateProvider(appConfig: IAppConfigProvider) {

    private val feeRateKit = FeeRateKit(
            FeeProviderConfig(infuraProjectId = appConfig.infuraProjectId,
                    infuraProjectSecret = appConfig.infuraProjectSecret)
    )

    fun bitcoinFeeRates(): Single<List<FeeRateInfo>> {
        return feeRateKit.bitcoin().map { feeRates(it) }
    }

    fun litecoinFeeRates(): Single<List<FeeRateInfo>> {
        return feeRateKit.litecoin().map { feeRates(it) }
    }

    fun bitcoinCashFeeRates(): Single<List<FeeRateInfo>> {
        return feeRateKit.bitcoinCash().map { feeRates(it) }
    }

    fun ethereumGasPrice(): Single<List<FeeRateInfo>> {
        return feeRateKit.ethereum().map { feeRates(it) }
    }

    fun dashFeeRates(): Single<List<FeeRateInfo>> {
        return feeRateKit.dash().map { feeRates(it) }
    }

    private fun feeRates(feeRate: FeeRate): List<FeeRateInfo> {
        val feeRatesInfoList = mutableListOf<FeeRateInfo>()
        feeRatesInfoList.add(FeeRateInfo(FeeRatePriority.LOW, feeRate.lowPriority, feeRate.lowPriorityDuration))
        feeRatesInfoList.add(FeeRateInfo(FeeRatePriority.MEDIUM, feeRate.mediumPriority, feeRate.mediumPriorityDuration))
        feeRatesInfoList.add(FeeRateInfo(FeeRatePriority.HIGH, feeRate.highPriority, feeRate.highPriorityDuration))

        return feeRatesInfoList
    }
}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): Single<List<FeeRateInfo>> {
        return feeRateProvider.bitcoinFeeRates()
    }
}

class LitecoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): Single<List<FeeRateInfo>> {
        return feeRateProvider.litecoinFeeRates()
    }
}

class BitcoinCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): Single<List<FeeRateInfo>> {
        return feeRateProvider.bitcoinCashFeeRates()
    }
}

class EthereumFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): Single<List<FeeRateInfo>> {
        return feeRateProvider.ethereumGasPrice()
    }
}

class DashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): Single<List<FeeRateInfo>> {
        return feeRateProvider.dashFeeRates()
    }

}
