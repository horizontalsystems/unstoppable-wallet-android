package io.horizontalsystems.bankwallet.modules.launcher

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class LaunchViewModel : ViewModel(), LaunchModule.IView, LaunchModule.IRouter {

    lateinit var delegate: LaunchModule.IViewDelegate

    val showNoDeviceLockWarning = SingleLiveEvent<Void>()
    val openWelcomeModule = SingleLiveEvent<Void>()
    val openMainModule = SingleLiveEvent<Void>()
    val openUnlockModule = SingleLiveEvent<Void>()

    fun init() {
        LaunchModule.init(this, this)
        delegate.viewDidLoad()
    }

    // IView

    override fun showNoDeviceLockWarning() {
        showNoDeviceLockWarning.call()
    }

    // IRouter

    override fun openWelcomeModule() {
        openWelcomeModule.call()
    }

    override fun openMainModule() {
        openMainModule.call()
    }

    override fun openUnlockModule() {
        openUnlockModule.call()
    }

}
