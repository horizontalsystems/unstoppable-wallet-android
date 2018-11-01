package bitcoin.wallet.modules.newpin.edit

import bitcoin.wallet.core.App
import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.modules.newpin.PinInteractor
import bitcoin.wallet.modules.newpin.PinViewModel

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
