package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import io.horizontalsystems.feeratekit.FeeRate
import io.horizontalsystems.feeratekit.FeeRateKit

class FeeRateProvider(context: Context, appConfig: IAppConfigProvider) : FeeRateKit.Listener {

    private val feeRateKit = FeeRateKit(
            infuraProjectId = appConfig.infuraProjectId,
            infuraProjectSecret = appConfig.infuraProjectSecret,
            context = context,
            listener = this
    )

    override fun onRefresh(rates: List<FeeRate>) {

    }

    fun bitcoinFeeRates(): List<FeeRateInfo> {
        return feeRates(feeRateKit.bitcoin())
    }

    fun bitcoinCashFeeRates(): List<FeeRateInfo> {
        return feeRates(feeRateKit.bitcoinCash())
    }

    fun ethereumFeeRates(): List<FeeRateInfo> {
        return feeRates(feeRateKit.ethereum())
    }

    fun dashFeeRates(): List<FeeRateInfo> {
        return feeRates(feeRateKit.dash())
    }

    private fun feeRates(feeRate: FeeRate): List<FeeRateInfo> {
        val feeRatesInfoList = mutableListOf<FeeRateInfo>()
        // TODO take duration from FeeRate
        feeRatesInfoList.add(FeeRateInfo(FeeRatePriority.LOW, feeRate.lowPriority, 30 * 60L))
        feeRatesInfoList.add(FeeRateInfo(FeeRatePriority.MEDIUM, feeRate.mediumPriority, 10 * 60L))
        feeRatesInfoList.add(FeeRateInfo(FeeRatePriority.HIGH, feeRate.mediumPriority, 5 * 60L))

        return feeRatesInfoList
    }
}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): List<FeeRateInfo> {
        return feeRateProvider.bitcoinFeeRates()
    }
}

class BitcoinCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): List<FeeRateInfo> {
        return feeRateProvider.bitcoinCashFeeRates()
    }
}

class EthereumFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): List<FeeRateInfo> {
        return feeRateProvider.ethereumFeeRates()
    }
}

class DashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRates(): List<FeeRateInfo> {
        return feeRateProvider.dashFeeRates()
    }

}
