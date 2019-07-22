package io.horizontalsystems.bankwallet.modules.settings.managekeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Account

class ManageKeysViewModel : ViewModel(), ManageKeysModule.View, ManageKeysModule.Router {

    val showItemsEvent = SingleLiveEvent<List<ManageAccountItem>>()
    val closeLiveEvent = SingleLiveEvent<Void>()
    val confirmUnlinkEvent = SingleLiveEvent<Account>()
    val startBackupModuleLiveEvent = SingleLiveEvent<Account>()
    val startRestoreWordsLiveEvent = SingleLiveEvent<Unit>()
    val startRestoreEosLiveEvent = SingleLiveEvent<Unit>()

    lateinit var delegate: ManageKeysModule.ViewDelegate

    fun init() {
        ManageKeysModule.init(this, this)
        delegate.viewDidLoad()
    }

    fun confirmUnlink(account: Account) {
        confirmUnlinkEvent.value = account
    }

    //  View

    override fun show(items: List<ManageAccountItem>) {
        showItemsEvent.postValue(items)
    }

    //  Router

    override fun startBackupModule(account: Account) {
        startBackupModuleLiveEvent.postValue(account)
    }

    override fun startRestoreWords() {
        startRestoreWordsLiveEvent.call()
    }

    override fun startRestoreEos() {
        startRestoreEosLiveEvent.call()
    }

    override fun close() {
        closeLiveEvent.call()
    }

    //  ViewModel

    override fun onCleared() {
        delegate.onClear()
    }
}
