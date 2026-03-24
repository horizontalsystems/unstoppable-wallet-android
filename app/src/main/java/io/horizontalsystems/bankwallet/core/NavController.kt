package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.bankwallet.modules.pin.SetPinFragment
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.subscriptions.core.IPaidAction
import java.util.UUID

fun NavBackStack<HSScreen>.slideFromRight(screen: HSScreen) {
//    TODO("xxx nav3")
//    navigate(resId, args, navOptions)
}

fun NavBackStack<HSScreen>.slideFromBottom(screen: HSScreen) {
//    TODO("xxx nav3")
//    navigate(resId, args, navOptions)
}

fun NavBackStack<HSScreen>.authorizedAction(action: () -> Unit) {
    if (App.pinComponent.isPinSet) {
        slideFromBottomForResult<ConfirmPinFragment.Result>(ConfirmPinFragment()) {
            if (it.success) {
                action.invoke()
            }
        }
    } else {
        action.invoke()
    }
}

fun NavBackStack<HSScreen>.paidAction(paidAction: IPaidAction, block: () -> Unit) {
//    TODO("xxx nav3")
//    if (UserSubscriptionManager.isActionAllowed(paidAction)) {
//        block.invoke()
//    } else {
//        val premiumFeature = PremiumFeature.getFeature(paidAction)
//        slideFromBottom(
//            R.id.defenseSystemFeatureDialog,
//            DefenseSystemFeatureDialog.Input(premiumFeature)
//        )
//    }
}

fun NavBackStack<HSScreen>.navigateWithTermsAccepted(action: () -> Unit) {
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

fun NavBackStack<HSScreen>.ensurePinSet(descriptionResId: Int, action: () -> Unit) {
    if (App.pinComponent.isPinSet) {
        action.invoke()
    } else {
        slideFromRightForResult<SetPinFragment.Result>(SetPinFragment(SetPinFragment.Input(descriptionResId))) {
            action.invoke()
        }
    }
}

fun <T: Parcelable> NavBackStack<HSScreen>.slideFromBottomForResult(screen: HSScreen, input: Parcelable? = null, onResult: (T) -> Unit) = Unit

fun <T: Parcelable> NavBackStack<HSScreen>.slideFromBottomForResult(screen: HSScreen, onResult: (T) -> Unit) = Unit

fun <T: Parcelable> NavBackStack<HSScreen>.slideFromRightForResult(screen: HSScreen, onResult: (T) -> Unit) {
//    TODO("xxx nav3")
}

private fun <T : Parcelable> NavBackStack<HSScreen>.navigateForResult(
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
//    TODO("xxx nav3")
//    navigate(resId, bundle, navOptions)
}

private fun <T: Parcelable> NavBackStack<HSScreen>.getNavigationResultX(key: String, onResult: (T) -> Unit) {
//    currentBackStackEntry?.let { backStackEntry ->
//        backStackEntry.savedStateHandle.getLiveData<T>(key).observe(backStackEntry) {
//            onResult.invoke(it)
//
//            backStackEntry.savedStateHandle.remove<T>(key)
//        }
//    }
}

inline fun <reified T: Parcelable> NavBackStack<HSScreen>.getInput() : T? {
    TODO()
//    return currentBackStackEntry?.arguments?.getInputX()
}

inline fun <reified T: Parcelable> Bundle.getInputX() : T? {
    return parcelable("input")
}

inline fun <reified T: Parcelable> NavBackStack<HSScreen>.requireInput() : T {
    return getInput()!!
}

fun <T: Parcelable> NavBackStack<HSScreen>.setNavigationResultX(result: T) {
//    val resultKey = currentBackStackEntry?.arguments?.getString("resultKey")
//
//    if (resultKey == null) {
//        Log.w("AAA", "No key registered to set the result")
//    } else {
//        previousBackStackEntry?.savedStateHandle?.set(resultKey, result)
//    }
}
