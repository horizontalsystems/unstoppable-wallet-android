package io.horizontalsystems.bankwallet.modules.pin.set

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.pin.PinInteractor
import io.horizontalsystems.bankwallet.modules.pin.PinViewModel

object SetPinModule {

    interface ISetPinRouter {
        fun navigateToMain()
        fun dismissModuleWithSuccess()
    }

    fun init(view: PinViewModel, router: ISetPinRouter) {

        val interactor = PinInteractor(App.pinManager)
        val presenter = SetPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
