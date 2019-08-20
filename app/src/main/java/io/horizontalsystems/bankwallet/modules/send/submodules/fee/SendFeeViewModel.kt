package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin

class SendFeeViewModel : ViewModel(), SendFeeModule.IView {

    lateinit var delegate: SendFeeModule.IViewDelegate

    val primaryFee = MutableLiveData<String?>()
    val secondaryFee = MutableLiveData<String?>()
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

    override fun setInsufficientFeeBalanceError(insufficientFeeBalance: SendFeeModule.InsufficientFeeBalance?) {
        insufficientFeeBalanceError.value = insufficientFeeBalance
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
