package io.horizontalsystems.bankwallet.modules.pin.set

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.modules.pin.PinInteractor
import io.horizontalsystems.bankwallet.modules.pin.PinViewModel

object SetPinModule {

    interface ISetPinRouter {
        fun navigateToMain()
        fun dismissModuleWithSuccess()
        fun dismissModuleWithCancel()
    }

    fun init(view: PinViewModel, router: ISetPinRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {

        val interactor = PinInteractor(App.pinManager, App.authManager, keystoreSafeExecute)
        val presenter = SetPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
