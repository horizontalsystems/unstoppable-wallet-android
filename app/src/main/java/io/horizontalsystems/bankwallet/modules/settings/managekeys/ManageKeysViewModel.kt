package io.horizontalsystems.bankwallet.modules.settings.managekeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Account

class ManageKeysViewModel : ViewModel(), ManageKeysModule.View, ManageKeysModule.Router {

    val showItemsEvent = SingleLiveEvent<List<ManageAccountItem>>()
    val showErrorEvent = SingleLiveEvent<Exception>()
    val confirmUnlinkEvent = SingleLiveEvent<ManageAccountItem>()
    val confirmCreateEvent = SingleLiveEvent<Pair<String, String>>()
    val confirmBackupEvent = SingleLiveEvent<String>()
    val startBackupModuleLiveEvent = SingleLiveEvent<Account>()
    val startRestoreWordsLiveEvent = SingleLiveEvent<Unit>()
    val startRestoreEosLiveEvent = SingleLiveEvent<Unit>()
    val closeLiveEvent = SingleLiveEvent<Void>()

    lateinit var delegate: ManageKeysModule.ViewDelegate

    fun init() {
        ManageKeysModule.init(this, this)
        delegate.viewDidLoad()
    }

    //  View

    override fun show(items: List<ManageAccountItem>) {
        showItemsEvent.postValue(items)
    }

    override fun showCreateConfirmation(title: String, coinCodes: String) {
        confirmCreateEvent.postValue(Pair(title, coinCodes))
    }

    override fun showBackupConfirmation(title: String) {
        confirmBackupEvent.postValue(title)
    }

    override fun showUnlinkConfirmation(accountItem: ManageAccountItem) {
        confirmUnlinkEvent.value = accountItem
    }

    override fun showError(error: Exception) {
        showErrorEvent.postValue(error)
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
