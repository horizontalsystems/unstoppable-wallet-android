package io.horizontalsystems.keystore

import android.os.Parcelable
import io.horizontalsystems.core.CoreApp
import kotlinx.android.parcel.Parcelize

object KeyStoreModule {

    const val MODE = "mode"

    interface IView {
        fun showNoSystemLockWarning()
        fun showInvalidKeyWarning()
        fun promptUserAuthentication()
        fun showDeviceIsRootedWarning()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onCloseInvalidKeyWarning()
        fun onAuthenticationCanceled()
        fun onAuthenticationSuccess()
    }

    interface IInteractor {
        fun resetApp()
        fun removeKey()
    }

    interface IInteractorDelegate

    interface IRouter {
        fun openLaunchModule()
        fun closeApplication()
    }

    fun init(view: KeyStoreViewModel, router: IRouter, mode: ModeType) {
        val interactor = KeyStoreInteractor(CoreApp.systemInfoManager, CoreApp.keyStoreManager)
        val presenter = KeyStorePresenter(interactor, router, mode)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    @Parcelize
    enum class ModeType : Parcelable {
        NoSystemLock,
        InvalidKey,
        UserAuthentication,
        DeviceIsRooted
    }

}
