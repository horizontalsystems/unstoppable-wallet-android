package io.horizontalsystems.bankwallet.modules.send

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IAdapter

class SendViewModel : ViewModel(), SendModule.IRouter {

    lateinit var delegate: SendModule.IViewDelegate
    val addressLiveData = MutableLiveData<String>()
    val primaryAmountLiveData = MutableLiveData<String>()
    val secondaryAmountHintLiveData = MutableLiveData<String>()
    val startScanLiveEvent = SingleLiveEvent<Unit>()
    val showSuccessLiveEvent = SingleLiveEvent<Unit>()
    val showAddressWarningLiveEvent = MutableLiveData<Boolean>()
    val showErrorLiveData = MutableLiveData<Int>()

    fun init(adapter: IAdapter) {
        SendModule.init(this, this, adapter)
        delegate.onViewDidLoad()
    }

//    override fun setAddress(address: String) {
//       addressLiveData.value = address
//    }
//
//    override fun setAmount(amount: String?) {
//        primaryAmountLiveData.value = amount
//    }
//
//    override fun setAmountHint(hint: String) {
//        secondaryAmountHintLiveData.value = hint
//    }
//
//    override fun showError(error: Int) {
//        showErrorLiveData.value = error
//    }
//
//    override fun showSuccess() {
//        showSuccessLiveEvent.call()
//    }
//
//    override fun showAddressWarning(show: Boolean) {
//        showAddressWarningLiveEvent.value = show
//    }
//
//    // --- IRouter methods ---
//
//    override fun startScan() {
//        startScanLiveEvent.call()
//    }

}
