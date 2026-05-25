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
import io.horizontalsystems.bankwallet.modules.pin.ConfirmPinPage
import io.horizontalsystems.bankwallet.modules.pin.SetPinPage
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsPage
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import java.util.UUID
import kotlin.reflect.KClass

class HSNavigation(val backStack: NavBackStack<HSPage>) {

    fun slideFromRight(screen: HSPage) {
        screen.navType = NavigationType.SlideFromRight
        backStack.add(screen)
    }

    fun slideFromBottom(screen: HSPage) {
        screen.navType = NavigationType.SlideFromBottom
        backStack.add(screen)
    }

    fun removeLastOrNull() {
        backStack.removeLastOrNull()
    }

    fun navigateWithTermsAccepted(
        screen: HSPage,
        navigationType: NavigationType,
        statPageFrom: StatPage,
        statPageTo: StatPage
    ) {
        if (!App.termsManager.allTermsAccepted) {
            slideFromBottom(TermsPage(screen, statPageFrom, statPageTo, navigationType))
        } else {
            when (navigationType) {
                NavigationType.SlideFromBottom -> slideFromBottom(screen)
                NavigationType.SlideFromRight -> slideFromRight(screen)
            }
            stat(page = statPageFrom, event = StatEvent.Open(statPageTo))
        }
    }

    @Composable
    inline fun <reified VM : ViewModel> viewModelForScreen(klass: KClass<out HSPage>) : VM {
        return viewModelForScreen(klass.simpleName ?: "HSScreen")
    }

    @Composable
    inline fun <reified VM : ViewModel> viewModelForScreen(contentKey: String) : VM {
        return viewModel(
            viewModelStoreOwner = rememberChildViewModelStoreOwner(contentKey),
        )
    }

    fun add(element: HSPage): Boolean {
        return backStack.add(element)
    }


    fun removeLastUntil(klass: KClass<out HSPage>, inclusive: Boolean) {
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
        ResultEffect<SetPinPage.Result>(resultKeyUuid = uuid) {
            action.invoke()
        }

        return if (App.pinComponent.isPinSet) {
            {
                action.invoke()
            }
        } else {
            {
                val screen = SetPinPage(
                    SetPinPage.Input(
                        descriptionResId
                    )
                )

                screen.resultKey = uuid
                add(screen)
            }
        }
    }

    @Composable
    inline fun <reified T> slideFromBottomForResult(
        crossinline screenBuilder: () -> HSPage,
        crossinline onResult: (T) -> Unit
    ): () -> Unit {
        return slideForResult(NavigationType.SlideFromBottom, screenBuilder, onResult)
    }

    @Composable
    inline fun <reified T> slideFromRightForResult(
        crossinline screenBuilder: () -> HSPage,
        crossinline onResult: (T) -> Unit
    ): () -> Unit {
        return slideForResult(NavigationType.SlideFromRight, screenBuilder, onResult)
    }

    @Composable
    fun authorizedAction(action: () -> Unit): () -> Unit {
        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        ResultEffect<ConfirmPinPage.Result>(resultKeyUuid = uuid) {
            if (it.success) {
                action.invoke()
            }
        }

        return if (App.pinComponent.isPinSet) {
            {
                val screen = ConfirmPinPage
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
    inline fun <reified T> slideForResult(
        navigationType: NavigationType,
        crossinline screenBuilder: () -> HSPage,
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

    fun lastOrNull(): HSPage? {
        return backStack.lastOrNull()
    }
}
