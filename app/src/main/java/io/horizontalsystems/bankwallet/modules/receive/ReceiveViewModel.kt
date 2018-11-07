package io.horizontalsystems.bankwallet.modules.receive

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceiveViewModel : ViewModel(), ReceiveModule.IView {

    lateinit var delegate: ReceiveModule.IViewDelegate

    val showAddressesLiveData = MutableLiveData<List<AddressItem>>()
    val showErrorLiveData = MutableLiveData<Int>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    fun init(adapterId: String) {
        ReceiveModule.init(this, adapterId)
        delegate.viewDidLoad()
    }

    override fun showAddresses(addresses: List<AddressItem>) {
        showAddressesLiveData.value = addresses
    }

    override fun showError(error: Int) {
        showErrorLiveData.value = error
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

}
