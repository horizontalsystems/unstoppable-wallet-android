package bitcoin.wallet.modules.restore

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.R
import bitcoin.wallet.SingleLiveEvent

class RestoreViewModel : ViewModel(), RestoreModule.IView, RestoreModule.IRouter {

    lateinit var delegate: RestoreModule.IViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val navigateToMainScreenLiveEvent = SingleLiveEvent<Void>()

    fun init() {
        RestoreModule.initModule(this, this)
    }

    override fun showInvalidWordsError() {
        errorLiveData.value = R.string.error
    }

    override fun navigateToMain() {
        navigateToMainScreenLiveEvent.call()
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
