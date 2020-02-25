package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.MutableLiveData

class SendAddressView : SendAddressModule.IView {

    val addressText = MutableLiveData<String?>()
    val error = MutableLiveData<Exception?>()
    val addressInputEditable = MutableLiveData<Boolean>()

    override fun setAddress(address: String?) {
        addressText.postValue(address)
    }

    override fun setAddressError(error: Exception?) {
        this.error.postValue(error)
    }

    override fun setAddressInputAsEditable(editable: Boolean) {
        addressInputEditable.postValue(editable)
    }
}
