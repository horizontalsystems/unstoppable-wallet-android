package io.horizontalsystems.bankwallet.core

import android.os.Parcelable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.bankwallet.modules.pin.SetPinFragment
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.subscriptions.core.IPaidAction

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
        slideFromBottomForResult<TermsFragment.Result>(TermsFragment()) { result ->
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

fun <T: Parcelable> NavBackStack<HSScreen>.slideFromBottomForResult(screen: HSScreen, onResult: (T) -> Unit) {
    add(screen)
//    TODO("xxx nav3")
}

fun <T: Parcelable> NavBackStack<HSScreen>.slideFromRightForResult(screen: HSScreen, onResult: (T) -> Unit) {
    add(screen)
//    TODO("xxx nav3")
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
