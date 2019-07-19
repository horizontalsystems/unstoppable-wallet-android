package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin

class ManageWalletsViewModel : ViewModel(), ManageWalletsModule.IView, ManageWalletsModule.IRouter {

    val coinsLoadedLiveEvent = SingleLiveEvent<Void>()
    val showRestoreKeyDialog = SingleLiveEvent<Coin>()
    val showCreateAndRestoreKeyDialog = SingleLiveEvent<Coin>()
    val startManageKeysLiveEvent = SingleLiveEvent<Coin>()
    val openRestoreWordsModule = SingleLiveEvent<Void>()
    val closeLiveDate = SingleLiveEvent<Void>()

    lateinit var delegate: ManageWalletsModule.IViewDelegate


    fun init() {
        ManageWalletsModule.init(this, this)
        delegate.viewDidLoad()
    }

    // View

    override fun updateCoins() {
        coinsLoadedLiveEvent.call()
    }

    override fun showRestoreKeyDialog(coin: Coin) {
        showRestoreKeyDialog.postValue(coin)
    }

    override fun showCreateAndRestoreKeyDialog(coin: Coin) {
        showCreateAndRestoreKeyDialog.postValue(coin)
    }

    override fun showFailedToSaveError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showFailedToCreateKey() {
        TODO("not implemented")
    }

    override fun showFailedToRestoreKey() {
        TODO("not implemented")
    }

    // Router

    override fun startManageKeysModule() {
        startManageKeysLiveEvent.call()
    }

    override fun close() {
        closeLiveDate.call()
    }

    override fun openRestoreWordsModule() {
        openRestoreWordsModule.call()
    }

    // View model

    override fun onCleared() {
        delegate.onClear()
    }

}
