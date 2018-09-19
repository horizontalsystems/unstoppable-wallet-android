package bitcoin.wallet.modules.pin

import android.content.Context
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.modules.pin.pinSubModules.SetPinConfirmationInteractor
import bitcoin.wallet.modules.pin.pinSubModules.SetPinConfirmationPresenter
import bitcoin.wallet.modules.pin.pinSubModules.SetPinInteractor
import bitcoin.wallet.modules.pin.pinSubModules.SetPinPresenter

object PinModule {

    const val pinLength = 6

    interface IView {
        fun setTitleEnterPin()
        fun setDescriptionEnterPin()
        fun setTitleEnterAgain()
        fun setDescriptionEnterAgain()

        fun highlightPinMask(length: Int)

        fun showErrorShortPinLength()
        fun showErrorPinsDontMatch()
        fun showErrorFailedToSavePin()
        fun showSuccessPinSet()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onEnterDigit(digit: Int)
        fun onClickDelete()
        fun onClickDone()
    }

    interface IInteractor {
        fun submit(pin: String)
    }

    interface IInteractorDelegate {
        fun goToPinConfirmation(pin: String)
        fun onErrorShortPinLength()
        fun onDidPinSet()
        fun onErrorPinsDontMatch()
        fun onErrorFailedToSavePin()
    }

    interface IRouter {
        fun goToPinConfirmation(pin: String)
    }

    fun init(view: PinViewModel, router: IRouter, interactionType: PinInteractionType, enteredPin: String) {

        val storage: ILocalStorage = Factory.preferencesManager

        val interactor = when (interactionType) {
            PinInteractionType.SET_PIN -> SetPinInteractor()
            PinInteractionType.SET_PIN_CONFIRM -> SetPinConfirmationInteractor(enteredPin, storage)
        }
        val presenter = when (interactionType) {
            PinInteractionType.SET_PIN -> SetPinPresenter(interactor, router)
            PinInteractionType.SET_PIN_CONFIRM -> SetPinConfirmationPresenter(interactor, router)
        }

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }


    fun startForSetPin(context: Context) {
        PinActivity.start(context, PinInteractionType.SET_PIN)
    }

    fun startForSetPinConfirm(context: Context, enteredPin: String) {
        PinActivity.start(context, PinInteractionType.SET_PIN_CONFIRM, enteredPin)
    }

}

enum class PinInteractionType {
    SET_PIN,
    SET_PIN_CONFIRM
}
