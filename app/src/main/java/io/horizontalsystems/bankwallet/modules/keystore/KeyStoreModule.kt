package io.horizontalsystems.bankwallet.modules.keystore

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import kotlinx.android.parcel.Parcelize

object KeyStoreModule {

    const val MODE = "mode"

    interface IView {
        fun showNoSystemLockWarning()
        fun showInvalidKeyWarning()
        fun promptUserAuthentication()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onCloseInvalidKeyWarning()
        fun onAuthenticationCanceled()
        fun onAuthenticationSuccess()
    }

    interface IInteractor {
        val isSystemLockOff: Boolean
        val isKeyInvalidated: Boolean
        val isUserNotAuthenticated: Boolean

        fun resetApp()
        fun removeKey()
    }

    interface IInteractorDelegate

    interface IRouter {
        fun openLaunchModule()
        fun closeApplication()
    }

    fun init(view: KeyStoreViewModel, router: IRouter, mode: ModeType) {
        val interactor = KeyStoreInteractor(App.accountManager, App.walletManager, App.localStorage, App.systemInfoManager, App.keyStoreManager)
        val presenter = KeyStorePresenter(interactor, router, mode)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun startForNoSystemLock(context: Context) {
        start(context, ModeType.NoSystemLock)
    }

    fun startForInvalidKey(context: Context) {
        start(context, ModeType.InvalidKey)
    }

    fun startForUserAuthentication(context: Context) {
        start(context, ModeType.UserAuthentication)
    }

    private fun start(context: Context, mode: ModeType) {
        val intent = Intent(context, KeyStoreActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putParcelableExtra(MODE, mode)
        context.startActivity(intent)
    }

    @Parcelize
    enum class ModeType : Parcelable {
        NoSystemLock,
        InvalidKey,
        UserAuthentication
    }

}
