package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.bankwallet.modules.pin.SetPinFragment
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.parcelable
import java.util.UUID

fun NavController.slideFromRight(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_right)
        .build()

    navigate(resId, args, navOptions)
}

fun NavController.slideFromBottom(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_bottom)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_bottom)
        .build()

    navigate(resId, args, navOptions)
}

fun NavController.authorizedAction(action: () -> Unit) {
    if (App.pinComponent.isPinSet) {
        slideFromBottomForResult<ConfirmPinFragment.Result>(R.id.confirmPinFragment) {
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
        getNavigationResult(TermsFragment.resultBundleKey) { bundle ->
            val agreedToTerms = bundle.getInt(TermsFragment.requestResultKey)

            if (agreedToTerms == TermsFragment.RESULT_OK) {
                action.invoke()
            }
        }
        slideFromBottom(R.id.termsFragment)
    } else {
        action.invoke()
    }
}

fun NavController.ensurePinSet(descriptionResId: Int, action: () -> Unit) {
    if (App.pinComponent.isPinSet) {
        action.invoke()
    } else {
        slideFromRightForResult<SetPinFragment.Result>(R.id.setPinFragment, SetPinFragment.Input(descriptionResId)) {
            action.invoke()
        }
    }
}

fun <T: Parcelable> NavController.slideFromBottomForResult(
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

fun <T: Parcelable> NavController.slideFromRightForResult(
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

private fun <T: Parcelable> NavController.getNavigationResultX(key: String, onResult: (T) -> Unit) {
    currentBackStackEntry?.let { backStackEntry ->
        backStackEntry.savedStateHandle.getLiveData<T>(key).observe(backStackEntry) {
            onResult.invoke(it)

            backStackEntry.savedStateHandle.remove<T>(key)
        }
    }
}

inline fun <reified T: Parcelable> NavController.getInput() : T? {
    return currentBackStackEntry?.arguments?.parcelable("input")
}

fun <T: Parcelable> NavController.setNavigationResultX(result: T, destinationId: Int? = null) {
    val resultKey = currentBackStackEntry?.arguments?.getString("resultKey")

    val backStackEntry = when (destinationId) {
        null -> previousBackStackEntry
        else -> currentBackStack.value.findLast { it.destination.id == destinationId }
    }

    resultKey?.let {
        backStackEntry?.savedStateHandle?.set(resultKey, result)
    }
}
