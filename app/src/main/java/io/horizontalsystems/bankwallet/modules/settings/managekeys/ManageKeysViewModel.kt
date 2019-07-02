package io.horizontalsystems.bankwallet.modules.settings.managekeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.Account

class ManageKeysViewModel : ViewModel(), ManageKeysModule.View, ManageKeysModule.Router {

    val showItemsEvent = SingleLiveEvent<List<Account>>()
    val closeLiveEvent = SingleLiveEvent<Void>()
    val unlinkAccountEvent = SingleLiveEvent<Account>()

    lateinit var delegate: ManageKeysModule.ViewDelegate

    fun init() {
        ManageKeysModule.init(this, this)
        delegate.viewDidLoad()
    }

    fun onUnlink(account: Account) {
        unlinkAccountEvent.value = account
    }

    //  View

    override fun show(items: List<Account>) {
        showItemsEvent.value = items
    }

    //  Router

    override fun close() {
        closeLiveEvent.call()
    }

    //  ViewModel

    override fun onCleared() {
        delegate.onClear()
    }
}
