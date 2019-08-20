package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin

class SendAddressViewModel : ViewModel(), SendAddressModule.IView {

    lateinit var delegate: SendAddressModule.IViewDelegate

    val addressTextLiveData = MutableLiveData<String?>()
    val errorLiveData = MutableLiveData<Exception?>()
    val pasteButtonEnabledLiveData = MutableLiveData<Boolean>()

    fun init(coin: Coin, moduleDelegate: SendAddressModule.IAddressModuleDelegate?): SendAddressPresenter {
        return SendAddressModule.init(this, coin, moduleDelegate)
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

}
