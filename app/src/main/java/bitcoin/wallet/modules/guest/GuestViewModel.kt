package bitcoin.wallet.modules.guest

import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent

class GuestViewModel: ViewModel(), GuestModule.IView, GuestModule.IRouter {

    lateinit var delegate: GuestModule.IViewDelegate

    val openBackupScreenLiveEvent = SingleLiveEvent<Void>()
    val openRestoreWalletScreenLiveEvent = SingleLiveEvent<Void>()
    val authenticateToCreateWallet = SingleLiveEvent<Void>()
    val showErrorDialog = SingleLiveEvent<Void>()

    fun init() {
        GuestModule.init(this, this)
    }

    // router
    override fun navigateToBackupRoutingToMain() {
        openBackupScreenLiveEvent.call()
    }

    override fun navigateToRestore() {
        openRestoreWalletScreenLiveEvent.call()
    }

    override fun authenticateToCreateWallet() {
        authenticateToCreateWallet.call()
    }

    override fun showError() {
        showErrorDialog.call()
    }

}
