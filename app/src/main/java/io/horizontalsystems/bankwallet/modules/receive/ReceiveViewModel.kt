package io.horizontalsystems.bankwallet.modules.receive

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.Coin

class ReceiveViewModel : ViewModel(), ReceiveModule.IView {

    lateinit var delegate: ReceiveModule.IViewDelegate

    val showAddressesLiveData = MutableLiveData<List<AddressItem>>()
    val showErrorLiveData = MutableLiveData<Int>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    fun init(coin: Coin) {
        ReceiveModule.init(this, coin)
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
