package io.horizontalsystems.bankwallet.core

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.subscriptions.core.IPaidAction

fun NavBackStack<HSScreen>.slideFromRight(screen: HSScreen) {
    add(screen)
//    TODO("xxx nav3")
}

fun NavBackStack<HSScreen>.slideFromBottom(screen: HSScreen) {
    add(screen)
//    TODO("xxx nav3")
}

@Composable
fun NavBackStack<HSScreen>.authorizedAction(action: () -> Unit): () -> Unit {
    return if (App.pinComponent.isPinSet) {
        slideFromBottomForResult<ConfirmPinFragment.Result>(ConfirmPinFragment()) {
            if (it.success) {
                action.invoke()
            }
        }
    } else {
        {
            action.invoke()
        }
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
//    TODO("xxx nav3")
//    if (!App.termsManager.allTermsAccepted) {
//        slideFromBottomForResult<TermsFragment.Result>(TermsFragment()) { result ->
//            if (result.termsAccepted) {
//                action.invoke()
//            }
//        }
//    } else {
//        action.invoke()
//    }
}

fun NavBackStack<HSScreen>.ensurePinSet(descriptionResId: Int, action: () -> Unit) {
    if (App.pinComponent.isPinSet) {
        action.invoke()
    } else {
//        TODO("xxx nav3")
//        slideFromRightForResult<SetPinFragment.Result>(SetPinFragment(SetPinFragment.Input(descriptionResId))) {
//            action.invoke()
//        }
    }
}

@Composable
inline fun <reified T> NavBackStack<HSScreen>.slideFromBottomForResult(
    screen: HSScreen,
    crossinline onResult: (T) -> Unit
): () -> Unit {
    ResultEffect<T> {
        onResult.invoke(it)
    }
    return {
        add(screen)
    }
}

@Composable
inline fun <reified T> NavBackStack<HSScreen>.slideFromRightForResult(
    screen: HSScreen,
    crossinline onResult: (T) -> Unit
): () -> Unit {
    ResultEffect<T> {
        onResult.invoke(it)
    }
    return {
        add(screen)
    }
}
