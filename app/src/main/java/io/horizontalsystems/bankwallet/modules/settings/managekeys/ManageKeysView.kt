package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.core.SingleLiveEvent

class ManageKeysView : ManageKeysModule.IView {

    val showItemsEvent = SingleLiveEvent<List<ManageAccountItem>>()
    val confirmUnlinkEvent = SingleLiveEvent<ManageAccountItem>()
    val confirmBackupEvent = SingleLiveEvent<ManageAccountItem>()

    override fun show(items: List<ManageAccountItem>) {
        showItemsEvent.postValue(items)
    }

    override fun showBackupConfirmation(accountItem: ManageAccountItem) {
        confirmBackupEvent.postValue(accountItem)
    }

    override fun showUnlinkConfirmation(accountItem: ManageAccountItem) {
        confirmUnlinkEvent.value = accountItem
    }
}
