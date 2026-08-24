package io.horizontalsystems.walletkit.modules.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.NavigationType
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.pin.ConfirmPinPage
import io.horizontalsystems.walletkit.modules.pin.SetPinPage
import io.horizontalsystems.walletkit.modules.settings.terms.TermsPage
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionHavHostPage
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import java.util.UUID
import kotlin.reflect.KClass

class HSNavigation(val backStack: NavBackStack<HSPage>) {

    fun slideFromRight(screen: HSPage) {
        screen.navType = NavigationType.SlideFromRight
        backStack.add(screen)
    }

    fun slideFromRightForResult(screen: HSPage, resultKey: String) {
        screen.resultKey = resultKey
        screen.navType = NavigationType.SlideFromRight
        backStack.add(screen)
    }

    fun slideFromBottom(screen: HSPage) {
        screen.navType = NavigationType.SlideFromBottom
        backStack.add(screen)
    }

    fun removeLastOrNull() {
        // Never pop the root entry: NavDisplay requires a non-empty backstack,
        // and rapid double back events (toolbar arrow double-tap or system back
        // during the pop animation) can otherwise drain the stack and crash.
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun navigateWithTermsAccepted(
        screen: HSPage,
        navigationType: NavigationType,
        statPageFrom: StatPage,
        statPageTo: StatPage
    ) {
        if (!App.termsManager.allTermsAccepted) {
            slideFromBottom(TermsPage(TermsPage.Input(screen, statPageFrom, statPageTo, navigationType)))
        } else {
            when (navigationType) {
                NavigationType.SlideFromBottom -> slideFromBottom(screen)
                NavigationType.SlideFromRight -> slideFromRight(screen)
            }
            stat(page = statPageFrom, event = StatEvent.Open(statPageTo))
        }
    }

    @Composable
    inline fun <reified VM : ViewModel> viewModelForScreen(
        klass: KClass<out HSPage>,
        factory: ViewModelProvider.Factory? = null,
    ) : VM {
        return viewModelForScreen(klass.simpleName ?: "HSScreen", factory)
    }

    @Composable
    // The factory makes the lookup restore-safe: after process death the shared
    // store is empty, and without a factory the default one instantiates the
    // ViewModel reflectively and crashes on constructor parameters.
    inline fun <reified VM : ViewModel> viewModelForScreen(
        contentKey: String,
        factory: ViewModelProvider.Factory? = null,
    ) : VM {
        return viewModel(
            viewModelStoreOwner = rememberChildViewModelStoreOwner(contentKey),
            factory = factory,
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
            // index 0 is the root entry; removing it would empty the backstack.
            if (inclusive && index > 0) {
                backStack.removeAt(index)
            }
        }
    }

    fun paidAction(paidAction: IPaidAction, block: () -> Unit) {
        if (UserSubscriptionManager.isActionAllowed(paidAction)) {
            block.invoke()
        } else {
            slideFromBottom(BuySubscriptionHavHostPage)
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
