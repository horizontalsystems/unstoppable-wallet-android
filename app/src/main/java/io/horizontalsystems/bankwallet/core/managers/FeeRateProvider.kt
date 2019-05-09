package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRate
import io.horizontalsystems.feeratekit.FeeRateKit

class FeeRateProvider(context: Context) : IFeeRateProvider, FeeRateKit.Listener {

    private val feeRateKit = FeeRateKit(context, this)

    override fun ethereumGasPrice(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.ethereum(), priority)
    }

    override fun bitcoinFeeRate(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.bitcoin(), priority)
    }

    override fun bitcoinCashFeeRate(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.bitcoinCash(), priority)
    }

    override fun dashFeeRate(priority: FeeRatePriority): Long {
        return feeRate(feeRateKit.dash(), priority)
    }

    override fun onRefresh(rates: List<FeeRate>) {

    }

    private fun feeRate(feeRate: FeeRate, priority: FeeRatePriority): Long {
        return when (priority) {
            FeeRatePriority.LOWEST -> feeRate.lowPriority
            FeeRatePriority.LOW -> (feeRate.lowPriority + feeRate.mediumPriority) / 2
            FeeRatePriority.MEDIUM -> feeRate.mediumPriority
            FeeRatePriority.HIGH -> (feeRate.mediumPriority + feeRate.highPriority) / 2
            FeeRatePriority.HIGHEST -> feeRate.highPriority
        }
    }
}
