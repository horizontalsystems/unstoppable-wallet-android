package cash.p.terminal.core

import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.modules.pin.ConfirmPinFragment
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.pin.SetPinFragment
import cash.p.terminal.modules.settings.terms.TermsFragment
import io.horizontalsystems.core.slideFromBottomForResult
import io.horizontalsystems.core.slideFromRightForResult

fun NavController.authorizedAction(
    input: ConfirmPinFragment.InputConfirm? = null,
    action: () -> Unit
) {
    val needEnterPin = when (input?.pinType) {
        PinType.REGULAR, PinType.DURESS, null -> App.pinComponent.isPinSet
        PinType.TRANSFER -> getKoinInstance<ILocalStorage>().transferPasscodeEnabled
        PinType.TRANSACTIONS_HIDE -> App.pinComponent.isPinSet || getKoinInstance<TransactionHiddenManager>().transactionHiddenFlow.value.transactionAutoHidePinExists
    }
    if (needEnterPin) {
        slideFromBottomForResult<ConfirmPinFragment.Result>(
            resId = R.id.confirmPinFragment,
            input = input
        ) {
            if (it.success) {
                action.invoke()
            }
        }
    } else {
        action.invoke()
    }
}

fun NavController.navigateWithTermsAccepted(action: () -> Unit) {
    if (!App.termsManager.allTermsAccepted) {
        slideFromBottomForResult<TermsFragment.Result>(R.id.termsFragment) { result ->
            if (result.termsAccepted) {
                action.invoke()
            }
        }
    } else {
        action.invoke()
    }
}

fun NavController.ensurePinSet(descriptionResId: Int, action: () -> Unit) {
    if (App.pinComponent.isPinSet) {
        action.invoke()
    } else {
        slideFromRightForResult<SetPinFragment.Result>(
            R.id.setPinFragment,
            SetPinFragment.Input(descriptionResId, PinType.REGULAR)
        ) {
            action.invoke()
        }
    }
}
