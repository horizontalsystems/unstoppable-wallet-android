package io.horizontalsystems.bankwallet.modules.restore.eos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class RestoreEosViewModel : ViewModel(), RestoreEosModule.IView, RestoreEosModule.IRouter {

    lateinit var delegate: RestoreEosModule.IViewDelegate

    val errorLiveEvent = MutableLiveData<Int>()
    val finishLiveEvent = SingleLiveEvent<Pair<String, String>>()

    fun init() {
        RestoreEosModule.init(this, this)
    }

    //  View

    override fun showError(resId: Int) {
        errorLiveEvent.postValue(resId)
    }

    //  Router

    override fun finishWithSuccess(accountName: String, privateKey: String) {
        finishLiveEvent.value = Pair(accountName, privateKey)
    }
}
