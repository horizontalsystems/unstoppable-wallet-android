package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin

class ManageWalletsViewModel : ViewModel(), ManageWalletsModule.IView, ManageWalletsModule.IRouter {

    val coinsLoadedLiveEvent = SingleLiveEvent<Void>()
    val showNoAccountLiveEvent = SingleLiveEvent<Coin>()
    val startManageKeysLiveEvent = SingleLiveEvent<Coin>()
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

    override fun showNoAccount(coin: Coin) {
        showNoAccountLiveEvent.postValue(coin)
    }

    override fun showFailedToSaveError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // Router

    override fun startManageKeysModule() {
        startManageKeysLiveEvent.call()
    }

    override fun close() {
        closeLiveDate.call()
    }

    // View model

    override fun onCleared() {
        delegate.onClear()
    }

}
