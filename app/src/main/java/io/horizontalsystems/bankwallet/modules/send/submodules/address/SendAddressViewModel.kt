package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin

class SendAddressViewModel : ViewModel(), SendAddressModule.IView {

    lateinit var delegate: SendAddressModule.IViewDelegate

    val addressText = MutableLiveData<String?>()
    val error = MutableLiveData<Exception?>()
    val pasteButtonEnabled = MutableLiveData<Boolean>()

    fun init(coin: Coin, moduleDelegate: SendAddressModule.IAddressModuleDelegate?): SendAddressPresenter {
        return SendAddressModule.init(this, coin, moduleDelegate)
    }

    override fun setAddress(address: String?) {
        addressText.value = address
    }

    override fun setAddressError(error: Exception?) {
        this.error.value = error
    }

    override fun setPasteButtonState(enabled: Boolean) {
        pasteButtonEnabled.value = enabled
    }

}
