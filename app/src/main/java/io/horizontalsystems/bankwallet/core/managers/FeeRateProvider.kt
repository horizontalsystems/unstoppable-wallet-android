package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.horizontalsystems.feeratekit.model.FeeRate
import io.reactivex.Single

class FeeRateProvider(context: Context, appConfig: IAppConfigProvider) : FeeRateKit.Listener {

    private val feeRateKit = FeeRateKit(
            FeeProviderConfig(infuraProjectId = appConfig.infuraProjectId,
                              infuraProjectSecret = appConfig.infuraProjectSecret,
                              btcCoreRpcUrl = appConfig.btcCoreRpcUrl,
                              btcCoreRpcUSer =  appConfig.btcCoreRpcUser,
                              btcCoreRpcPassword = appConfig.btcCoreRpcPassword),
            context = context,
            listener = this
    )

    fun bitcoinFeeRates(): Single<List<FeeRateInfo>> {
        return feeRateKit.bitcoin().map { feeRates(it) }
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

    override fun onRefresh(rate: FeeRate) {
        TODO("not implemented")
    }
}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): Single<List<FeeRateInfo>> {
        return feeRateProvider.bitcoinFeeRates()
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
