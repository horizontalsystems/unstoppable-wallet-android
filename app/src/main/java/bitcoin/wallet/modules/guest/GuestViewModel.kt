package bitcoin.wallet.modules.guest

import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent

class GuestViewModel: ViewModel(), GuestModule.IView, GuestModule.IRouter {

    override lateinit var presenter: GuestModule.IPresenter

    val openBackupScreenLiveEvent = SingleLiveEvent<Void>()
    val openRestoreWalletScreenLiveEvent = SingleLiveEvent<Void>()

    fun init() {
        GuestModule.init(this, this)

        presenter.start()
    }

    // router
    override fun openBackupScreen() {
        openBackupScreenLiveEvent.call()
    }

    override fun openRestoreWalletScreen() {
        openRestoreWalletScreenLiveEvent.call()
    }
}
