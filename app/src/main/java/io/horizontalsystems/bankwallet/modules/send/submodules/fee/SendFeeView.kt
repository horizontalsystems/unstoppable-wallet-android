package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.modules.send.SendModule

class SendFeeView : SendFeeModule.IView {

    val showAdjustableFeeMenu = MutableLiveData<Boolean>()
    val primaryFee = MutableLiveData<String?>()
    val secondaryFee = MutableLiveData<String?>()
    val feePriority = MutableLiveData<FeeRatePriority>()
    val showFeePriorityOptions = MutableLiveData<List<SendFeeModule.FeeRateInfoViewItem>>()
    val showCustomFeePriority = SingleLiveEvent<Boolean>()
    val setCustomFeeParams = SingleLiveEvent<Triple<Int, IntRange, String?>>()
    val insufficientFeeBalanceError = SingleLiveEvent<SendFeeModule.InsufficientFeeBalance?>()
    val setLoading = MutableLiveData<Boolean>()
    val setError = MutableLiveData<Exception>()

    override fun setAdjustableFeeVisible(visible: Boolean) {
        showAdjustableFeeMenu.postValue(visible)
    }

    override fun setPrimaryFee(feeAmount: String?) {
        primaryFee.postValue(feeAmount)
    }

    override fun setSecondaryFee(feeAmount: String?) {
        secondaryFee.postValue(feeAmount)
    }

    override fun setFeePriority(priority: FeeRatePriority) {
        feePriority.postValue(priority)
    }

    override fun showFeeRatePrioritySelector(feeRates: List<SendFeeModule.FeeRateInfoViewItem>) {
        showFeePriorityOptions.value = feeRates
    }

    override fun showCustomFeePriority(show: Boolean) {
        showCustomFeePriority.postValue(show)
    }

    override fun setCustomFeeParams(value: Int, range: IntRange, label: String?) {
        setCustomFeeParams.postValue(Triple(value, range, label))
    }

    override fun setLoading(loading: Boolean) {
        this.setLoading.postValue(loading)
    }

    override fun setFee(fee: SendModule.AmountInfo, convertedFee: SendModule.AmountInfo?) {
    }

    override fun setError(error: Exception?) {
        this.setError.postValue(error)
    }

    override fun setInsufficientFeeBalanceError(insufficientFeeBalance: SendFeeModule.InsufficientFeeBalance?) {
        insufficientFeeBalanceError.postValue(insufficientFeeBalance)
    }
}
