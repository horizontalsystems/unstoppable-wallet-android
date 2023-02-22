package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.modules.send.SendErrorFetchFeeRateFailed
import io.horizontalsystems.bankwallet.modules.send.SendWarningLowFee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class SendBitcoinFeeRateService(private val feeRateProvider: IFeeRateProvider) {

    private var feeRatePriorities: List<FeeRatePriority> = listOf()
    var feeRateChangeable: Boolean = false
        private set

    private var feeRatePriority: FeeRatePriority = FeeRatePriority.RECOMMENDED
    private var feeRate: Long? = null
    private var feeRateCaution: HSCaution? = null

    private var lowFeeRate: Long? = null
    private var mediumFeeRate: Long? = null

    private val _stateFlow = MutableStateFlow(
        State(
            feeRate = feeRate,
            feeRateCaution = feeRateCaution,
            feeRatePriority = feeRatePriority,
            canBeSend = false
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    suspend fun start() {
        withContext(Dispatchers.IO) {
            try {
                feeRatePriorities = feeRateProvider.feeRatePriorityList
                feeRateChangeable = feeRatePriorities.isNotEmpty()
                lowFeeRate = feeRateProvider.getFeeRate(FeeRatePriority.LOW)
                mediumFeeRate = feeRateProvider.getFeeRate(FeeRatePriority.RECOMMENDED)
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
        val tmpFeeRateCaution = feeRateCaution

        _stateFlow.update {
            State(
                feeRate = feeRate,
                feeRateCaution = feeRateCaution,
                feeRatePriority = feeRatePriority,
                canBeSend = tmpFeeRateCaution == null || tmpFeeRateCaution.isWarning()
            )
        }
    }

    private fun validateFeeRate() {
        val tmpLowFeeRate = lowFeeRate
        val tmpMediumFeeRate = mediumFeeRate
        val tmpFeeRate = feeRate

        feeRateCaution = if (tmpFeeRate == null) {
            SendErrorFetchFeeRateFailed
        } else if (tmpLowFeeRate != null && tmpMediumFeeRate != null && tmpLowFeeRate < tmpMediumFeeRate && tmpFeeRate <= tmpLowFeeRate) {
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
        val canBeSend: Boolean,
    )
}
