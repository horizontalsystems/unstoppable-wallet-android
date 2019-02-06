package io.horizontalsystems.bankwallet.modules.managecoins

import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class ManageCoinsViewModel : ViewModel(), ManageCoinsModule.IView, ManageCoinsModule.IRouter {

    val coinsLoadedLiveEvent = SingleLiveEvent<Void>()
    val closeLiveDate = SingleLiveEvent<Void>()

    lateinit var delegate: ManageCoinsModule.IViewDelegate


    fun init() {
        ManageCoinsModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun updateCoins() {
        coinsLoadedLiveEvent.call()
    }

    override fun close() {
        closeLiveDate.call()
    }

    override fun showFailedToSaveError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
