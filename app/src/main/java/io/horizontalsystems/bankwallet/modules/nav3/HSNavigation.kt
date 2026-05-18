package io.horizontalsystems.bankwallet.modules.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.NavigationType
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinFragment
import io.horizontalsystems.bankwallet.modules.pin.SetPinFragment
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import java.util.UUID
import kotlin.reflect.KClass

class HSNavigation(val backStack: NavBackStack<HSScreen>) {

    fun slideFromRight(screen: HSScreen) {
        screen.navType = NavigationType.SlideFromRight
        backStack.add(screen)
    }

    fun slideFromBottom(screen: HSScreen) {
        screen.navType = NavigationType.SlideFromBottom
        backStack.add(screen)
    }

    fun removeLastOrNull() {
        backStack.removeLastOrNull()
    }

    fun navigateWithTermsAccepted(
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
    fun authorizedAction(action: () -> Unit): () -> Unit {
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
    inline fun <reified VM : ViewModel> viewModelForScreen(klass: KClass<out HSScreen>) : VM {
        val hSScreen = checkNotNull(backStack.findLast { it::class == klass })

        return viewModel(
            viewModelStoreOwner = rememberChildViewModelStoreOwner(hSScreen.contentKey()),
        )
    }

    fun add(element: HSScreen): Boolean {
        return backStack.add(element)
    }


    fun removeLastUntil(klass: KClass<out HSScreen>, inclusive: Boolean) {
        val index = backStack.indexOfLast { it::class == klass }
        if (index != -1) {
            for (i in backStack.lastIndex downTo (index + 1)) {
                backStack.removeAt(i)
            }
            if (inclusive) {
                backStack.removeAt(index)
            }
        }
    }

    fun paidAction(paidAction: IPaidAction, block: () -> Unit) {
        if (UserSubscriptionManager.isActionAllowed(paidAction)) {
            block.invoke()
        } else {
            val premiumFeature = PremiumFeature.getFeature(paidAction)
            slideFromBottom(
                DefenseSystemFeatureDialog(DefenseSystemFeatureDialog.Input(premiumFeature))
            )
        }
    }

    @Composable
    fun ensurePinSet(descriptionResId: Int, action: () -> Unit): () -> Unit {
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
    inline fun <reified VM : ViewModel> viewModelForPrevScreen() : VM {
        val hSScreen = backStack[backStack.lastIndex - 1]

        return viewModel(
            viewModelStoreOwner = rememberChildViewModelStoreOwner(hSScreen.contentKey()),
        )
    }

    @Composable
    inline fun <reified T> slideFromBottomForResult(
        screen: HSScreen,
        crossinline onResult: (T) -> Unit
    ): () -> Unit {
        return slideForResult(screen, NavigationType.SlideFromBottom, onResult)
    }

    @Composable
    inline fun <reified T> slideFromRightForResult(
        screen: HSScreen,
        crossinline onResult: (T) -> Unit
    ): () -> Unit {
        return slideForResult(screen, NavigationType.SlideFromRight, onResult)
    }

    @Composable
    inline fun <reified T> slideForResult(
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
            screen.navType = navigationType
            add(screen)
        }
    }

    @Composable
    inline fun <reified T> slideForResult(
        navigationType: NavigationType,
        crossinline screenBuilder: () -> HSScreen,
        crossinline onResult: (T) -> Unit
    ): () -> Unit {
        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        ResultEffect<T>(resultKeyUuid = uuid) {
            onResult.invoke(it)
        }
        return {
            val screen = screenBuilder()
            screen.resultKey = uuid
            screen.navType = navigationType
            add(screen)
        }
    }

    fun lastOrNull(): HSScreen? {
        return backStack.lastOrNull()
    }
}
