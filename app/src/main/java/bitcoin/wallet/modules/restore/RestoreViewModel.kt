package bitcoin.wallet.modules.restore

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.R
import bitcoin.wallet.SingleLiveEvent

class RestoreViewModel : ViewModel(), RestoreModule.IView, RestoreModule.IRouter, RestoreModule.IKeyStoreSafeExecute {

    lateinit var delegate: RestoreModule.IViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val navigateToMainScreenLiveEvent = SingleLiveEvent<Void>()
    val authenticateToRestoreWallet = SingleLiveEvent<Void>()
    val keyStoreSafeExecute = SingleLiveEvent<Triple<Runnable, Runnable?, Runnable?>>()

    fun init() {
        RestoreModule.init(this, this, this)
    }

    override fun showInvalidWordsError() {
        errorLiveData.value = R.string.error
    }

    override fun navigateToMain() {
        navigateToMainScreenLiveEvent.call()
    }

    override fun authenticateToRestoreWallet() {
        authenticateToRestoreWallet.call()
    }

    override fun safeExecute(action: Runnable, onSuccess: Runnable?, onFailure: Runnable?) {
        keyStoreSafeExecute.value = Triple(action, onSuccess, onFailure)
    }
}
