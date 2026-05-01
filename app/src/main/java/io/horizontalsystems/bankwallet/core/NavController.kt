package io.horizontalsystems.bankwallet.core

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager

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
    if (UserSubscriptionManager.isActionAllowed(paidAction)) {
        block.invoke()
    } else {
        val premiumFeature = PremiumFeature.getFeature(paidAction)
        slideFromBottom(
            DefenseSystemFeatureDialog(DefenseSystemFeatureDialog.Input(premiumFeature))
        )
    }
}

enum class NavigationType {
    SlideFromBottom,
    SlideFromRight,
}

fun NavBackStack<HSScreen>.navigateWithTermsAccepted(
    screen: HSScreen,
    navigationType: NavigationType,
    statPageFrom: StatPage,
    statPageTo: StatPage
) {
    if (!App.termsManager.allTermsAccepted) {
        slideFromBottom(TermsFragment(screen, statPageFrom, statPageTo, navigationType))
    } else {
        when (navigationType) {
            NavigationType.SlideFromBottom -> slideFromBottom(screen)
            NavigationType.SlideFromRight -> slideFromRight(screen)
        }
        stat(page = statPageFrom, event = StatEvent.Open(statPageTo))
    }
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
