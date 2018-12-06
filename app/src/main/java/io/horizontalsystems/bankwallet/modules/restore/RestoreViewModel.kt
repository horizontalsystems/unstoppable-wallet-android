package io.horizontalsystems.bankwallet.modules.restore

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute

class RestoreViewModel : ViewModel(), RestoreModule.IView, RestoreModule.IRouter, IKeyStoreSafeExecute {

    lateinit var delegate: RestoreModule.IViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val navigateToSetPinLiveEvent = SingleLiveEvent<Void>()
    val keyStoreSafeExecute = SingleLiveEvent<Triple<Runnable, Runnable?, Runnable?>>()

    fun init() {
        RestoreModule.init(this, this, this)
    }

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    override fun navigateToSetPin() {
        navigateToSetPinLiveEvent.call()
    }

    override fun safeExecute(action: Runnable, onSuccess: Runnable?, onFailure: Runnable?) {
        keyStoreSafeExecute.value = Triple(action, onSuccess, onFailure)
    }
}
