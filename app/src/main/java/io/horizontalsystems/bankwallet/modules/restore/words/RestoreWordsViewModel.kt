package io.horizontalsystems.bankwallet.modules.restore.words

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class RestoreWordsViewModel : ViewModel(), RestoreWordsModule.IView, RestoreWordsModule.IRouter {

    lateinit var delegate: RestoreWordsModule.IViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val startSyncModeModule = SingleLiveEvent<Unit>()

    fun init() {
        RestoreWordsModule.init(this, this)
    }

    // View

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    // Router

    override fun startSyncModeModule(words: List<String>) {
        startSyncModeModule.call()
    }
}
