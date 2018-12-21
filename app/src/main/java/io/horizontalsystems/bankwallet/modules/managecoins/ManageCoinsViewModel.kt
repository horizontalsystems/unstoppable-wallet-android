package io.horizontalsystems.bankwallet.modules.managecoins

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin

class ManageCoinsViewModel: ViewModel(), ManageCoinsModule.IView, ManageCoinsModule.IRouter {

    val coinsLoadedLiveEvent = SingleLiveEvent<Void>()
    val titleLiveDate = MutableLiveData<Int>()
    val closeLiveDate = SingleLiveEvent<Void>()

    lateinit var delegate: ManageCoinsModule.IViewDelegate


    fun init() {
        ManageCoinsModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun setTitle(title: Int) {
        titleLiveDate.value = title
    }

    override fun showCoins(enabledCoins: List<Coin>, disabledCoins: List<Coin>) {
        coinsLoadedLiveEvent.call()
    }

    override fun close() {
        closeLiveDate.call()
    }

    override fun showFailedToSaveError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
