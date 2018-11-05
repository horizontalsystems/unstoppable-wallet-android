package bitcoin.wallet.modules.pin.edit

import bitcoin.wallet.core.App
import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.modules.pin.PinViewModel

object EditPinModule {

    interface IEditPinRouter {
        fun dismiss()
    }

    fun init(view: PinViewModel, router: IEditPinRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {

        val interactor = PinInteractor(App.pinManager, keystoreSafeExecute)
        val presenter = EditPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
