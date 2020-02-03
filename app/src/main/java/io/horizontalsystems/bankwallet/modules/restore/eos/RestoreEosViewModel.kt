package io.horizontalsystems.bankwallet.modules.restore.eos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class RestoreEosViewModel : ViewModel(), RestoreEosModule.IView, RestoreEosModule.IRouter {

    lateinit var delegate: RestoreEosModule.IViewDelegate

    val startQRScanner = SingleLiveEvent<Unit>()
    val setAccount = MutableLiveData<String>()
    val setPrivateKey = MutableLiveData<String>()
    val errorLiveEvent = MutableLiveData<Int>()
    val finishLiveEvent = SingleLiveEvent<Pair<String, String>>()

    fun init() {
        RestoreEosModule.init(this, this)
    }

    //  View

    override fun setPrivateKey(key: String) {
        setPrivateKey.postValue(key)
    }

    override fun setAccount(account: String) {
        setAccount.postValue(account)
    }

    override fun showError(resId: Int) {
        errorLiveEvent.postValue(resId)
    }

    //  Router

    override fun startQRScanner() {
        startQRScanner.call()
    }

    override fun finishWithSuccess(accountName: String, privateKey: String) {
        finishLiveEvent.value = Pair(accountName, privateKey)
    }
}
