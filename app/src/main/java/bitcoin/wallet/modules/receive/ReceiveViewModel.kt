package bitcoin.wallet.modules.receive

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent
import bitcoin.wallet.modules.receive.viewitems.AddressItem

class ReceiveViewModel : ViewModel(), ReceiveModule.IView, ReceiveModule.IRouter {

    lateinit var delegate: ReceiveModule.IViewDelegate

    val showAddressesLiveData = MutableLiveData<List<AddressItem>>()
    val showErrorLiveData = MutableLiveData<Int>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val openShareViewLiveEvent = SingleLiveEvent<String>()

    fun init(adapterId: String) {
        ReceiveModule.init(this, this, adapterId)
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

    override fun openShareView(receiveAddress: String) {
        openShareViewLiveEvent.value = receiveAddress
    }

}
