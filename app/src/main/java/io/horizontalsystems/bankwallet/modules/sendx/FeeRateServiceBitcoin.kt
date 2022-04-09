package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class FeeRateServiceBitcoin(private val feeRateProvider: IFeeRateProvider) {

    var feeRatePriorities: List<FeeRatePriority> = listOf()
        private set
    var feeRateRange: ClosedRange<Long>? = null
        private set

    private var feeRatePriority: FeeRatePriority = FeeRatePriority.RECOMMENDED
    private var feeRate: Long? = null
    private var feeRateCaution: HSCaution? = null

    private var lowFeeRate: Long? = null

    private val _stateFlow = MutableStateFlow(
        State(
            feeRate = feeRate,
            feeRateCaution = feeRateCaution,
            feeRatePriority = feeRatePriority,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    suspend fun start() {
        withContext(Dispatchers.IO) {
            try {
                feeRatePriorities = feeRateProvider.feeRatePriorityList
                feeRateRange = feeRateProvider.getFeeRateRange()
                lowFeeRate = feeRateProvider.getFeeRate(FeeRatePriority.LOW)
            } catch (e: Exception) {

            }

            refreshFeeRate()
            validateFeeRate()
            emitState()
        }
    }

    suspend fun setFeeRatePriority(feeRatePriority: FeeRatePriority) {
        this.feeRatePriority = feeRatePriority

        refreshFeeRate()
        validateFeeRate()
        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                feeRate = feeRate,
                feeRateCaution = feeRateCaution,
                feeRatePriority = feeRatePriority,
            )
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

    data class State(
        val feeRate: Long?,
        val feeRateCaution: HSCaution?,
        val feeRatePriority: FeeRatePriority,
    )
}
