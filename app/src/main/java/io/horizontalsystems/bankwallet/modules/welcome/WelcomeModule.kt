package io.horizontalsystems.bankwallet.modules.welcome

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object WelcomeModule {

    interface IView {
        fun setAppVersion(appVersion: String)
    }

    interface IViewDelegate {
        fun createWalletDidClick()
        fun restoreWalletDidClick()
        fun viewDidLoad()
        fun openTorPage()
    }

    interface IInteractor {
        val appVersion: String
    }

    interface IRouter {
        fun openRestoreModule()
        fun openCreateWalletModule()
        fun openTorPage()
    }

    fun start(fragmentActivity: FragmentActivity, sharedLogo: View) {
        fragmentActivity.supportFragmentManager.commit {
            addSharedElement(sharedLogo, "welcome_wallet_logo")
            replace(R.id.fragmentContainerView, WelcomeFragment.instance())
        }
    }

    fun init(view: WelcomeViewModel, router: IRouter) {
        val interactor = WelcomeInteractor(App.systemInfoManager)
        val presenter = WelcomePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
    }

}
