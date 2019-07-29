package io.horizontalsystems.bankwallet.modules.pin.edit

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.pin.PinInteractor
import io.horizontalsystems.bankwallet.modules.pin.PinViewModel

object EditPinModule {

    interface IEditPinRouter {
        fun dismissModuleWithSuccess()
        fun dismissModuleWithCancel()
    }

    fun init(view: PinViewModel, router: IEditPinRouter) {

        val interactor = PinInteractor(App.pinManager)
        val presenter = EditPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
