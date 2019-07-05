package io.horizontalsystems.bankwallet.modules.restorewords

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class RestoreWordsViewModel : ViewModel(), RestoreWordsModule.IView, RestoreWordsModule.IRouter {

    lateinit var delegate: RestoreWordsModule.IViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val goToSetSyncModeLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        RestoreWordsModule.init(this, this)
    }

    // View

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    // Router

    override fun navigateToSetSyncMode(words: List<String>) {
        goToSetSyncModeLiveEvent.call()
    }

    override fun notifyRestored(accountType: AccountType, syncMode: SyncMode) {
        TODO("not implemented")
    }
}
