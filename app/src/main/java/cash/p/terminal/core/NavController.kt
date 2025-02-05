package cash.p.terminal.core

import android.os.Parcelable
import android.util.Log
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import cash.p.terminal.R
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.modules.pin.ConfirmPinFragment
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.pin.SetPinFragment
import cash.p.terminal.modules.settings.terms.TermsFragment
import java.util.UUID

fun NavController.slideFromBottom(@IdRes resId: Int, input: Parcelable? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_bottom)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_bottom)
        .build()

    val args = input?.let {
        bundleOf("input" to it)
    }
    navigate(resId, args, navOptions)
}

fun NavController.authorizedAction(
    input: ConfirmPinFragment.InputConfirm? = null,
    action: () -> Unit
) {
    val needEnterPin = when (input?.pinType) {
        PinType.REGULAR, PinType.DURESS, null -> App.pinComponent.isPinSet
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

fun <T : Parcelable> NavController.slideFromBottomForResult(
    @IdRes resId: Int,
    input: Parcelable? = null,
    onResult: (T) -> Unit
) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_bottom)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_bottom)
        .build()

    navigateForResult(resId, input, navOptions, onResult)
}

fun <T : Parcelable> NavController.slideFromRightForResult(
    @IdRes resId: Int,
    input: Parcelable? = null,
    onResult: (T) -> Unit
) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_right)
        .build()

    navigateForResult(resId, input, navOptions, onResult)
}

private fun <T : Parcelable> NavController.navigateForResult(
    resId: Int,
    input: Parcelable?,
    navOptions: NavOptions,
    onResult: (T) -> Unit,
) {
    val key = UUID.randomUUID().toString()
    getNavigationResultX(key, onResult)
    val bundle = bundleOf("resultKey" to key)
    input?.let {
        bundle.putParcelable("input", it)
    }
    navigate(resId, bundle, navOptions)
}

private fun <T : Parcelable> NavController.getNavigationResultX(
    key: String,
    onResult: (T) -> Unit
) {
    currentBackStackEntry?.let { backStackEntry ->
        backStackEntry.savedStateHandle.getLiveData<T>(key).observe(backStackEntry) {
            onResult.invoke(it)

            backStackEntry.savedStateHandle.remove<T>(key)
        }
    }
}

fun <T : Parcelable> NavController.setNavigationResultX(result: T) {
    val resultKey = currentBackStackEntry?.arguments?.getString("resultKey")

    if (resultKey == null) {
        Log.w("AAA", "No key registered to set the result")
    } else {
        previousBackStackEntry?.savedStateHandle?.set(resultKey, result)
    }
}
