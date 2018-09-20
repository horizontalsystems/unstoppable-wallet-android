package bitcoin.wallet.modules.pin

import android.content.Context
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISettingsManager
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.modules.pin.pinSubModules.*

object PinModule {

    const val pinLength = 6

    interface IView {
        fun setTitleForEnterPin()
        fun setDescriptionForEnterPin()
        fun setTitleForEnterAgain()
        fun setDescriptionForEnterAgain()
        fun setDescriptionForUnlock()

        fun highlightPinMask(length: Int)

        fun showErrorShortPinLength()
        fun showErrorPinsDontMatch()
        fun showErrorFailedToSavePin()
        fun showSuccessPinSet()
        fun hideToolbar()
        fun showErrorWrongPin()
        fun clearPinMaskWithDelay()
        fun showFingerprintDialog()
        fun minimizeApp()
        fun navigateToPrevPage()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onEnterDigit(digit: Int)
        fun onClickDelete()
        fun onClickDone()
        fun onBackPressed()
    }

    interface IInteractor {
        fun submit(pin: String)
        fun viewDidLoad()
        fun onBackPressed()
    }

    interface IInteractorDelegate {
        fun goToPinConfirmation(pin: String)
        fun onErrorShortPinLength()
        fun onDidPinSet()
        fun onErrorPinsDontMatch()
        fun onErrorFailedToSavePin()
        fun onCorrectPinSubmitted()
        fun onWrongPinSubmitted()
        fun onFingerprintEnabled()
        fun onMinimizeApp()
        fun onNavigateToPrevPage()
    }

    interface IRouter {
        fun goToPinConfirmation(pin: String)
        fun unlockWallet()
    }

    fun init(view: PinViewModel, router: IRouter, interactionType: PinInteractionType, enteredPin: String) {

        val storage: ILocalStorage = Factory.preferencesManager
        val settings: ISettingsManager = Factory.preferencesManager

        val interactor = when (interactionType) {
            PinInteractionType.SET_PIN -> SetPinInteractor()
            PinInteractionType.SET_PIN_CONFIRM -> SetPinConfirmationInteractor(enteredPin, storage)
            PinInteractionType.UNLOCK -> UnlockInteractor(storage, settings)
        }
        val presenter = when (interactionType) {
            PinInteractionType.SET_PIN -> SetPinPresenter(interactor, router)
            PinInteractionType.SET_PIN_CONFIRM -> SetPinConfirmationPresenter(interactor, router)
            PinInteractionType.UNLOCK -> UnlockPresenter(interactor, router)
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

    fun startForUnlock(context: Context) {
        PinActivity.start(context, PinInteractionType.UNLOCK)
    }

}

enum class PinInteractionType {
    SET_PIN,
    SET_PIN_CONFIRM,
    UNLOCK
}
