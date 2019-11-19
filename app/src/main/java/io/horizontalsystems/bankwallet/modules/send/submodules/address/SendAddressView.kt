package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.MutableLiveData

class SendAddressView : SendAddressModule.IView {

    val addressText = MutableLiveData<String?>()
    val error = MutableLiveData<Exception?>()
    val pasteButtonEnabled = MutableLiveData<Boolean>()

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
