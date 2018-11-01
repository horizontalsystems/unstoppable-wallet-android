package bitcoin.wallet.modules.newpin.set

import bitcoin.wallet.core.App
import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.modules.newpin.PinInteractor
import bitcoin.wallet.modules.newpin.PinViewModel

object SetPinModule {

    interface ISetPinRouter {
        fun navigateToMain()
    }

    fun init(view: PinViewModel, router: ISetPinRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {

        val interactor = PinInteractor(App.pinManager, keystoreSafeExecute)
        val presenter = SetPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
