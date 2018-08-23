package bitcoin.wallet.modules.receive

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent

class ReceiveViewModel : ViewModel(), ReceiveModule.IView, ReceiveModule.IRouter {

    lateinit var delegate: ReceiveModule.IViewDelegate

    val showAddressLiveData = MutableLiveData<String>()
    val showErrorLiveData = MutableLiveData<Int>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val openShareViewLiveEvent = SingleLiveEvent<String>()

    fun init(coinCode: String) {
        ReceiveModule.init(this, this, coinCode)
        delegate.viewDidLoad()
    }

    override fun showAddress(coinAddress: String) {
        showAddressLiveData.value = coinAddress
    }

    override fun showError(error: Int) {
        showErrorLiveData.value = error
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun openShareView(coinAddress: String) {
        openShareViewLiveEvent.value = coinAddress
    }

}
