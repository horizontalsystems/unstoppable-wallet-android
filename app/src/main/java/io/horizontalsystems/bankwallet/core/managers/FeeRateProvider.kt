package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRate
import io.horizontalsystems.feeratekit.FeeRateKit

class FeeRateProvider(context: Context, appConfig: IAppConfigProvider) : FeeRateKit.Listener {

    private val feeRateKit = FeeRateKit(
            infuraProjectId = appConfig.infuraProjectId,
            infuraProjectSecret = appConfig.infuraProjectSecret,
            context = context,
            listener = this
    )

    fun ethereumGasPrice(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.ethereum(), priority)
    }

    fun bitcoinFeeRate(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.bitcoin(), priority)
    }

    fun bitcoinCashFeeRate(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.bitcoinCash(), priority)
    }

    fun dashFeeRate(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.dash(), priority)
    }

    override fun onRefresh(rates: List<FeeRate>) {

    }

    private fun feeRate(feeRate: FeeRate, priority: FeeRatePriority): Long {
        return when (priority) {
            FeeRatePriority.LOWEST -> feeRate.safeLow()
            FeeRatePriority.LOW -> (feeRate.safeLow() + feeRate.safeMedium()) / 2
            FeeRatePriority.MEDIUM -> feeRate.safeMedium()
            FeeRatePriority.HIGH -> (feeRate.safeMedium() + feeRate.safeHigh()) / 2
            FeeRatePriority.HIGHEST -> feeRate.safeHigh()
        }
    }
}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRate(priority: FeeRatePriority): Long {
        return feeRateProvider.bitcoinFeeRate(priority)
    }
}

class BitcoinCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRate(priority: FeeRatePriority): Long {
        return feeRateProvider.bitcoinCashFeeRate(priority)
    }
}

class EthereumFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRate(priority: FeeRatePriority): Long {
        return feeRateProvider.ethereumGasPrice(priority)
    }
}

class DashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override fun feeRate(priority: FeeRatePriority): Long {
        return feeRateProvider.dashFeeRate(priority)
    }
}
