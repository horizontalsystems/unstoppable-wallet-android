package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceiveViewModel : ViewModel(), ReceiveModule.IView, ReceiveModule.IRouter {

    lateinit var delegate: ReceiveModule.IViewDelegate

    val showAddressLiveData = MutableLiveData<AddressItem>()
    val showErrorLiveData = MutableLiveData<Int>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val shareAddressLiveEvent = SingleLiveEvent<String>()

    fun init(wallet: Wallet) {
        ReceiveModule.init(wallet, this, this)
        delegate.viewDidLoad()
    }

    override fun showAddress(addresses: AddressItem) {
        showAddressLiveData.value = addresses
    }

    override fun showError(error: Int) {
        showErrorLiveData.value = error
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun shareAddress(address: String) {
        shareAddressLiveEvent.value = address
    }
}
