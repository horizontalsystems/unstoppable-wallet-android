package io.horizontalsystems.bankwallet.modules.welcome

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class WelcomeViewModel : ViewModel(), WelcomeModule.IView, WelcomeModule.IRouter {

    lateinit var delegate: WelcomeModule.IViewDelegate

    val openMainModule = SingleLiveEvent<Void>()
    val openRestoreModule = SingleLiveEvent<Void>()
    val showErrorDialog = SingleLiveEvent<Void>()
    val appVersionLiveData = MutableLiveData<String>()

    fun init() {
        WelcomeModule.init(this, this)
        delegate.viewDidLoad()
    }

    // IView

    override fun setAppVersion(appVersion: String) {
        appVersionLiveData.value = appVersion
    }

    override fun showError() {
        showErrorDialog.call()
    }

    // IRouter

    override fun openMainModule() {
        openMainModule.call()
    }

    override fun openRestoreModule() {
        openRestoreModule.call()
    }

}
