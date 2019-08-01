package io.horizontalsystems.bankwallet.modules.send.sendviews.address

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import java.math.BigDecimal

class SendAddressViewModel: ViewModel(), SendAddressModule.IView {

    lateinit var delegate: SendAddressModule.IViewDelegate

    val addressTextLiveData = MutableLiveData<String?>()
    val notifyMainViewModelOnAddressChangedLiveData = SingleLiveEvent<Unit>()
    val errorLiveData = MutableLiveData<Exception?>()
    val pasteButtonEnabledLiveData = MutableLiveData<Boolean>()
    val amountLiveData = MutableLiveData<BigDecimal>()
    val mainViewModelParseAddressLiveData = MutableLiveData<String>()

    fun init() {
        SendAddressModule.init(this)
    }

    override fun onAmountChange(amount: BigDecimal) {
        amountLiveData.value = amount
    }

    override fun setAddress(address: String?) {
        addressTextLiveData.value = address
    }

    override fun setAddressError(error: Exception?) {
        errorLiveData.value = error
    }

    override fun setPasteButtonState(enabled: Boolean) {
        pasteButtonEnabledLiveData.value = enabled
    }

    override fun notifyMainViewModelAddressUpdated() {
        notifyMainViewModelOnAddressChangedLiveData.call()
    }

    override fun parseAddressInMainViewModel(address: String) {
        mainViewModelParseAddressLiveData.value = address
    }
}
