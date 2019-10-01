package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.Coin

class SendFeeViewModel : ViewModel(), SendFeeModule.IView {

    lateinit var delegate: SendFeeModule.IViewDelegate

    val primaryFee = MutableLiveData<String?>()
    val secondaryFee = MutableLiveData<String?>()
    val duration = MutableLiveData<Long>()
    val feePriority = MutableLiveData<FeeRatePriority>()
    val showFeePriorityOptions = MutableLiveData<List<SendFeeModule.FeeRateInfoViewItem>>()
    val insufficientFeeBalanceError = SingleLiveEvent<SendFeeModule.InsufficientFeeBalance?>()

    fun init(coin: Coin, moduleDelegate: SendFeeModule.IFeeModuleDelegate?): SendFeeModule.IFeeModule {
        return SendFeeModule.init(this, coin, moduleDelegate)
    }

    override fun setPrimaryFee(feeAmount: String?) {
        primaryFee.value = feeAmount
    }

    override fun setSecondaryFee(feeAmount: String?) {
        secondaryFee.value = feeAmount
    }

    override fun setFeePriority(priority: FeeRatePriority) {
        feePriority.value = priority
    }

    override fun setDuration(duration: Long) {
        this.duration.value = duration
    }

    override fun showFeeRatePrioritySelector(feeRates: List<SendFeeModule.FeeRateInfoViewItem>) {
        showFeePriorityOptions.value = feeRates
    }

    override fun setInsufficientFeeBalanceError(insufficientFeeBalance: SendFeeModule.InsufficientFeeBalance?) {
        insufficientFeeBalanceError.value = insufficientFeeBalance
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
