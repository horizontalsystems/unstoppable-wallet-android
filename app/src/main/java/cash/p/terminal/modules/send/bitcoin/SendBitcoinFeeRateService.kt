package cash.p.terminal.modules.send.bitcoin

import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.IFeeRateProvider
import cash.p.terminal.modules.send.SendErrorFetchFeeRateFailed
import cash.p.terminal.modules.send.SendErrorLowFee
import cash.p.terminal.modules.send.SendWarningRiskOfGettingStuck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class SendBitcoinFeeRateService(private val feeRateProvider: IFeeRateProvider) {
    val feeRateChangeable = feeRateProvider.feeRateChangeable

    private var feeRate: Int? = null
    private var feeRateCaution: HSCaution? = null
    private var canBeSend = false

    private var recommendedFeeRate: Int? = null
    private var minimumFeeRate = 0

    private val _stateFlow = MutableStateFlow(
        State(
            feeRate = feeRate,
            feeRateCaution = feeRateCaution,
            canBeSend = canBeSend
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            val feeRates = feeRateProvider.getFeeRates()

            recommendedFeeRate = feeRates.recommended
            minimumFeeRate = feeRates.minimum
            feeRate = recommendedFeeRate
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        validateFeeRate()
        emitState()
    }

    fun setRecommendedAndMin(recommended: Int, minimum: Int) {
        recommendedFeeRate = recommended
        minimumFeeRate = minimum

        feeRate = recommendedFeeRate

        validateFeeRate()
        emitState()
    }

    fun setFeeRate(v: Int) {
        feeRate = v

        validateFeeRate()
        emitState()
    }

    fun reset() {
        feeRate = recommendedFeeRate

        validateFeeRate()
        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                feeRate = feeRate,
                feeRateCaution = feeRateCaution,
                canBeSend = canBeSend
            )
        }
    }

    private fun validateFeeRate() {
        val tmpFeeRate = feeRate
        val tmpRecommendedFeeRate = recommendedFeeRate

        when {
            tmpFeeRate == null -> {
                feeRateCaution = SendErrorFetchFeeRateFailed
                canBeSend = false
            }
            tmpFeeRate < minimumFeeRate -> {
                feeRateCaution = SendErrorLowFee
                canBeSend = true
            }
            tmpRecommendedFeeRate != null && tmpFeeRate < tmpRecommendedFeeRate -> {
                feeRateCaution = SendWarningRiskOfGettingStuck
                canBeSend = true
            }
            else -> {
                feeRateCaution = null
                canBeSend = true
            }
        }
    }

    data class State(
        val feeRate: Int?,
        val feeRateCaution: HSCaution?,
        val canBeSend: Boolean,
    )
}
