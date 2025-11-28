package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.bankwallet.modules.pin.SetPinFragment
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import java.util.UUID

fun NavController.slideFromRight(@IdRes resId: Int, input: Parcelable? = null, xxx: NavOptions.Builder.() -> Unit = { }) {
    val builder = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_right)

    xxx(builder)

    val navOptions = builder.build()

    val args = input?.let {
        bundleOf("input" to it)
    }
    navigate(resId, args, navOptions)
}

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

fun NavController.paidAction(paidAction: IPaidAction, block: () -> Unit) {
    if (UserSubscriptionManager.isActionAllowed(paidAction)) {
        block.invoke()
    } else {
        val premiumFeature = PremiumFeature.getFeature(paidAction)
        slideFromBottom(
            R.id.defenseSystemFeatureDialog,
            DefenseSystemFeatureDialog.Input(premiumFeature, true)
        )
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
    return currentBackStackEntry?.arguments?.getInputX()
}

inline fun <reified T: Parcelable> Bundle.getInputX() : T? {
    return parcelable("input")
}

inline fun <reified T: Parcelable> NavController.requireInput() : T {
    return getInput()!!
}

fun <T: Parcelable> NavController.setNavigationResultX(result: T) {
    val resultKey = currentBackStackEntry?.arguments?.getString("resultKey")

    if (resultKey == null) {
        Log.w("AAA", "No key registered to set the result")
    } else {
        previousBackStackEntry?.savedStateHandle?.set(resultKey, result)
    }
}
