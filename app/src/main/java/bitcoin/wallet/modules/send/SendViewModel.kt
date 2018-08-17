package bitcoin.wallet.modules.send

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent

class SendViewModel : ViewModel(), SendModule.IView, SendModule.IRouter {

    lateinit var delegate: SendModule.IViewDelegate
    val addressLiveData = MutableLiveData<String>()
    val primaryAmountLiveData = MutableLiveData<String>()
    val secondaryAmountHintLiveData = MutableLiveData<String>()
    val startScanLiveEvent = SingleLiveEvent<Unit>()
    val showSuccessLiveEvent = SingleLiveEvent<Unit>()
    val showErrorLiveData = MutableLiveData<Int>()

    fun init(coinCode: String) {
        SendModule.init(this, this, coinCode)
        delegate.onViewDidLoad()
    }

    override fun setAddress(address: String) {
       addressLiveData.value = address
    }

    override fun setAmount(amount: String?) {
        primaryAmountLiveData.value = amount
    }

    override fun setAmountHint(hint: String) {
        secondaryAmountHintLiveData.value = hint
    }

    override fun showError(error: Int) {
        showErrorLiveData.value = error
    }

    override fun showSuccess() {
        showSuccessLiveEvent.call()
    }

    // --- IRouter methods ---

    override fun startScan() {
        startScanLiveEvent.call()
    }

}
