package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue

class SendFeeViewModel : ViewModel(), SendFeeModule.IView {

    lateinit var delegate: SendFeeModule.IViewDelegate

    val primaryFeeLiveData = MutableLiveData<String?>()
    val secondaryFeeLiveData = MutableLiveData<String?>()
    val insufficientFeeBalanceErrorLiveEvent = SingleLiveEvent<CoinValue>()


    fun init(coin: Coin, moduleDelegate: SendFeeModule.IFeeModuleDelegate?): SendFeeModule.IFeeModule {
        return SendFeeModule.init(this, coin, moduleDelegate)
    }


    override fun setPrimaryFee(feeAmount: String?) {
        primaryFeeLiveData.value = feeAmount
    }

    override fun setSecondaryFee(feeAmount: String?) {
        secondaryFeeLiveData.value = feeAmount
    }

    override fun setInsufficientFeeBalanceError(feeCoinValue: CoinValue) {
        insufficientFeeBalanceErrorLiveEvent.value = feeCoinValue
    }
}
