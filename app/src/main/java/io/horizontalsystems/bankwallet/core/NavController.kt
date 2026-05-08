package io.horizontalsystems.bankwallet.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.bankwallet.modules.pin.SetPinFragment
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.serialization.Serializable
import java.util.UUID

fun NavBackStack<HSScreen>.slideFromRight(screen: HSScreen) {
    add(screen)
}

fun NavBackStack<HSScreen>.slideFromBottom(screen: HSScreen) {
    add(screen)
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

@Serializable
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

@Composable
fun NavBackStack<HSScreen>.authorizedAction(action: () -> Unit): () -> Unit {
    val uuid = rememberSaveable { UUID.randomUUID().toString() }
    ResultEffect<ConfirmPinFragment.Result>(resultKeyUuid = uuid) {
        if (it.success) {
            action.invoke()
        }
    }

    return if (App.pinComponent.isPinSet) {
        {
            val screen = ConfirmPinFragment
            screen.resultKey = uuid
            add(screen)
        }
    } else {
        {
            action.invoke()
        }
    }
}

@Composable
fun NavBackStack<HSScreen>.ensurePinSet(descriptionResId: Int, action: () -> Unit): () -> Unit {
    val uuid = rememberSaveable { UUID.randomUUID().toString() }
    ResultEffect<SetPinFragment.Result>(resultKeyUuid = uuid) {
        action.invoke()
    }

    return if (App.pinComponent.isPinSet) {
        {
            action.invoke()
        }
    } else {
        {
            val screen = SetPinFragment(
                SetPinFragment.Input(
                    descriptionResId
                )
            )

            screen.resultKey = uuid
            add(screen)
        }
    }
}

@Composable
inline fun <reified T> NavBackStack<HSScreen>.slideForResult(
    screen: HSScreen,
    navigationType: NavigationType,
    crossinline onResult: (T) -> Unit
): () -> Unit {
    val uuid = rememberSaveable { UUID.randomUUID().toString() }
    ResultEffect<T>(resultKeyUuid = uuid) {
        onResult.invoke(it)
    }
    return {
        screen.resultKey = uuid
        add(screen)
    }
}

@Composable
inline fun <reified T> NavBackStack<HSScreen>.slideFromBottomForResult(
    screen: HSScreen,
    crossinline onResult: (T) -> Unit
): () -> Unit {
    return slideForResult(screen, NavigationType.SlideFromBottom, onResult)
}

@Composable
inline fun <reified T> NavBackStack<HSScreen>.slideFromRightForResult(
    screen: HSScreen,
    crossinline onResult: (T) -> Unit
): () -> Unit {
    return slideForResult(screen, NavigationType.SlideFromRight, onResult)
}
