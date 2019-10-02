package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceiveView: ReceiveModule.IView {

    val showAddress = MutableLiveData<AddressItem>()
    val showError = MutableLiveData<Int>()
    val showCopied = SingleLiveEvent<Unit>()
    val setHintText = SingleLiveEvent<Int>()

    override fun showAddress(address: AddressItem) {
        showAddress.value = address
    }

    override fun showError(error: Int) {
        showError.value = error
    }

    override fun showCopied() {
        showCopied.call()
    }

    override fun setHint(hint: Int) {
        setHintText.value = hint
    }
}