package io.horizontalsystems.bankwallet.modules.managecoins

import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin

class ManageCoinsViewModel: ViewModel(), ManageCoinsModule.IView, ManageCoinsModule.IRouter {

    lateinit var delegate: ManageCoinsModule.IViewDelegate


    fun init() {
        ManageCoinsModule.init(this, this)
    }

    override fun showCoins(enabledCoins: List<Coin>, disabledCoins: List<Coin>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showFailedToSaveError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
