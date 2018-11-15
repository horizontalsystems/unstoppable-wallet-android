package io.horizontalsystems.bankwallet.modules.pin.edit

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.modules.pin.PinInteractor
import io.horizontalsystems.bankwallet.modules.pin.PinViewModel

object EditPinModule {

    interface IEditPinRouter {
        fun dismiss()
    }

    fun init(view: PinViewModel, router: IEditPinRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {

        val interactor = PinInteractor(App.pinManager, App.wordsManager, keystoreSafeExecute)
        val presenter = EditPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
