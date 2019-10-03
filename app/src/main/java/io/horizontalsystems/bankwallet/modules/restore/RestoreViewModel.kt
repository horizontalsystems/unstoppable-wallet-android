package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType

class RestoreViewModel : ViewModel(), RestoreModule.View, RestoreModule.Router {

    lateinit var delegate: RestoreModule.ViewDelegate

    val reloadLiveEvent = SingleLiveEvent<List<IPredefinedAccountType>>()
    val showErrorLiveEvent = SingleLiveEvent<Exception>()
    val startRestoreWordsLiveEvent = SingleLiveEvent<Pair<Int, Int>>()
    val startRestoreEosLiveEvent = SingleLiveEvent<Int>()
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        RestoreModule.init(this, this)
        delegate.viewDidLoad()
    }

    //  View

    override fun reload(items: List<IPredefinedAccountType>) {
        reloadLiveEvent.postValue(items)
    }

    override fun showError(ex: Exception) {
        showErrorLiveEvent.postValue(ex)
    }

    //  Router

    override fun startRestoreWordsModule(wordsCount: Int, titleRes: Int) {
        startRestoreWordsLiveEvent.postValue(Pair(wordsCount, titleRes))
    }

    override fun startRestoreEosModule(titleRes: Int) {
        startRestoreEosLiveEvent.postValue(titleRes)
    }

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
