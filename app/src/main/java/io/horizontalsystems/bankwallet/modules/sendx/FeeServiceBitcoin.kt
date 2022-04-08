package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class FeeServiceBitcoin(
    private val feeRateProvider: IFeeRateProvider,
    private val adapter: ISendBitcoinAdapter
) {
    var fee: BigDecimal? = null
        private set
    var feeRate: Long? = null
        private set
    var feeRateCaution: HSCaution? = null
        private set

    var feeRatePriorities: List<FeeRatePriority> = listOf()
        private set
    var feeRatePriority: FeeRatePriority = FeeRatePriority.RECOMMENDED
        private set
    var feeRateRange: ClosedRange<Long>? = null
        private set

    private var lowFeeRate: Long? = null

    private var amount: BigDecimal? = null
    private var validAddress: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    suspend fun init(amount: BigDecimal?, validAddress: Address?, pluginData: Map<Byte, IPluginData>?) {
        this.amount = amount
        this.validAddress = validAddress
        this.pluginData = pluginData

        withContext(Dispatchers.IO) {
            try {
                feeRatePriorities = feeRateProvider.feeRatePriorityList
                feeRateRange = feeRateProvider.getFeeRateRange()
                lowFeeRate = feeRateProvider.getFeeRate(FeeRatePriority.LOW)
            } catch (e: Exception) {

            }

            refreshFeeRate()
            validateFeeRate()
            refreshFee()
        }
    }

    private fun validateFeeRate() {
        val tmpLowFeeRate = lowFeeRate
        val tmpFeeRate = feeRate

        feeRateCaution = if (tmpFeeRate == null) {
            SendErrorFetchFeeRateFailed
        } else if (tmpLowFeeRate != null && tmpFeeRate <= tmpLowFeeRate) {
            SendWarningLowFee
        } else {
            null
        }
    }

    private suspend fun refreshFeeRate() {
        try {
            feeRate = feeRateProvider.getFeeRate(feeRatePriority)
        } catch (e: Exception) {
            feeRate = null
        }
    }

    private fun refreshFee() {
        val tmpAmount = amount
        val tmpFeeRate = feeRate

        fee = when {
            tmpAmount == null -> null
            tmpFeeRate == null -> null
            else -> adapter.fee(tmpAmount, tmpFeeRate, validAddress?.hex, pluginData)
        }
    }

    suspend fun setFeeRatePriority(feeRatePriority: FeeRatePriority) {
        this.feeRatePriority = feeRatePriority

        refreshFeeRate()
        validateFeeRate()

        refreshFee()
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFee()
    }

    fun setValidAddress(validAddress: Address?) {
        this.validAddress = validAddress

        refreshFee()
    }

    fun setPluginData(pluginData: Map<Byte, IPluginData>?) {
        this.pluginData = pluginData
    }

}
