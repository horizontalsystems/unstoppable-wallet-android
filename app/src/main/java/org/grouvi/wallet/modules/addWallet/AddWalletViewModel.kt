package org.grouvi.wallet.modules.addWallet

import android.arch.lifecycle.ViewModel
import org.grouvi.wallet.SingleLiveEvent

class AddWalletViewModel: ViewModel(), AddWalletModule.IView, AddWalletModule.IRouter {

    override lateinit var presenter: AddWalletModule.IPresenter

    val openBackupScreenLiveEvent = SingleLiveEvent<Void>()

    fun init() {
        AddWalletModule.init(this, this)

        presenter.start()
    }

    // router
    override fun openBackupScreen() {
        openBackupScreenLiveEvent.call()
    }
}
