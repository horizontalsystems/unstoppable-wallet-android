package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.modules.send.SendModule

class SendFeeView : SendFeeModule.IView {

    val primaryFee = MutableLiveData<String?>()
    val secondaryFee = MutableLiveData<String?>()
    val duration = MutableLiveData<Long>()
    val feePriority = MutableLiveData<FeeRatePriority>()
    val showFeePriorityOptions = MutableLiveData<List<SendFeeModule.FeeRateInfoViewItem>>()
    val insufficientFeeBalanceError = SingleLiveEvent<SendFeeModule.InsufficientFeeBalance?>()
    val setLoading = MutableLiveData<Boolean>()
    val setError = MutableLiveData<Exception>()

    override fun setPrimaryFee(feeAmount: String?) {
        primaryFee.postValue(feeAmount)
    }

    override fun setSecondaryFee(feeAmount: String?) {
        secondaryFee.postValue(feeAmount)
    }

    override fun setFeePriority(priority: FeeRatePriority) {
        feePriority.postValue(priority)
    }

    override fun setDuration(duration: Long) {
        this.duration.postValue(duration)
    }

    override fun showFeeRatePrioritySelector(feeRates: List<SendFeeModule.FeeRateInfoViewItem>) {
        showFeePriorityOptions.value = feeRates
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
