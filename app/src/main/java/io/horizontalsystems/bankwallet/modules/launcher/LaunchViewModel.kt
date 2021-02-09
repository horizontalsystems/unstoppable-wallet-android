package io.horizontalsystems.bankwallet.modules.launcher

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class LaunchViewModel : ViewModel(), LaunchModule.IView, LaunchModule.IRouter {

    lateinit var delegate: LaunchModule.IViewDelegate

    val openWelcomeModule = SingleLiveEvent<Void>()
    val openMainModule = SingleLiveEvent<Void>()
    val openUnlockModule = SingleLiveEvent<Void>()
    val openNoSystemLockModule = SingleLiveEvent<Void>()
    val openKeyInvalidatedModule = SingleLiveEvent<Void>()
    val openUserAuthenticationModule = SingleLiveEvent<Void>()
    val openDeviceIsRootedWarning = SingleLiveEvent<Void>()
    val closeApplication = SingleLiveEvent<Void>()

    fun init() {
        LaunchModule.init(this, this)
        delegate.viewDidLoad()
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

    override fun openNoSystemLockModule() {
        openNoSystemLockModule.call()
    }

    override fun openKeyInvalidatedModule() {
        openKeyInvalidatedModule.call()
    }

    override fun openUserAuthenticationModule() {
        openUserAuthenticationModule.call()
    }

    override fun openDeviceIsRootedWarning() {
        openDeviceIsRootedWarning.call()
    }

    override fun closeApplication() {
        closeApplication.call()
    }
}
