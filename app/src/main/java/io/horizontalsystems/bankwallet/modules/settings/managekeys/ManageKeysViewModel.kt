package io.horizontalsystems.bankwallet.modules.settings.managekeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper

class ManageKeysViewModel : ViewModel(), ManageKeysModule.View, ManageKeysModule.Router {

    val showItemsEvent = SingleLiveEvent<List<ManageAccountItem>>()
    val showErrorEvent = SingleLiveEvent<Exception>()
    val confirmUnlinkEvent = SingleLiveEvent<ManageAccountItem>()
    val confirmCreateEvent = SingleLiveEvent<ManageAccountItem>()
    val confirmBackupEvent = SingleLiveEvent<ManageAccountItem>()
    val startBackupModuleLiveEvent = SingleLiveEvent<ManageAccountItem>()
    val startRestoreWordsLiveEvent = SingleLiveEvent<Pair<Int,Int>>()
    val startRestoreEosLiveEvent = SingleLiveEvent<Int>()
    val closeLiveEvent = SingleLiveEvent<Void>()

    lateinit var delegate: ManageKeysModule.ViewDelegate

    fun init() {
        ManageKeysModule.init(this, this)
        delegate.viewDidLoad()
    }

    //  IView

    override fun show(items: List<ManageAccountItem>) {
        showItemsEvent.postValue(items)
    }

    override fun showCreateConfirmation(accountItem: ManageAccountItem) {
        confirmCreateEvent.postValue(accountItem)
    }

    override fun showBackupConfirmation(accountItem: ManageAccountItem) {
        confirmBackupEvent.postValue(accountItem)
    }

    override fun showUnlinkConfirmation(accountItem: ManageAccountItem) {
        confirmUnlinkEvent.value = accountItem
    }

    override fun showSuccess() {
        HudHelper.showSuccessMessage(R.string.Hud_Text_Done, 500)
    }

    override fun showError(error: Exception) {
        showErrorEvent.postValue(error)
    }

    //  Router

    override fun startBackupModule(accountItem: ManageAccountItem) {
        startBackupModuleLiveEvent.postValue(accountItem)
    }

    override fun startRestoreWords(wordsCount: Int, titleRes: Int) {
        startRestoreWordsLiveEvent.postValue(Pair(wordsCount, titleRes))
    }

    override fun startRestoreEos(titleRes: Int) {
        startRestoreEosLiveEvent.postValue(titleRes)
    }

    override fun close() {
        closeLiveEvent.call()
    }

    //  ViewModel

    override fun onCleared() {
        delegate.onClear()
    }
}
