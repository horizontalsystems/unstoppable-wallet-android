package io.horizontalsystems.bankwallet.modules.nav3

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.main.MainScreen
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureScreen
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsScreen
import io.horizontalsystems.bankwallet.modules.tonconnect.TonConnectSendRequestScreen
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlin.reflect.KClass

@Composable
fun Nav3(mainActivityViewModel: MainActivityViewModel) {
    val resultBus = remember { ResultEventBus() }

    val backStack = rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        NavBackStack<HSScreen>(MainScreen)
    }

    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<HSScreen>() }

    val currentScreen = backStack.lastOrNull()
    val activity = LocalActivity.current

    LaunchedEffect(currentScreen) {
        if (activity != null) {
            if (currentScreen?.screenshotEnabled == false) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    NavDisplay(
        entryDecorators = listOf(
            // Add the default decorators for managing scenes and saving state
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberSharedViewModelStoreNavEntryDecorator(),
            rememberResultEventBusNavEntryDecorator(),
        ),
        backStack = backStack,
        sceneStrategy = bottomSheetStrategy,
        entryProvider = { hSScreen ->
            NavEntry(
                key = hSScreen,
                contentKey = hSScreen.contentKey(),
                metadata = hSScreen.getMetadata(backStack)
            ) {
                if (currentScreen is MainScreen) {
                    currentScreen.mainActivityViewModel = mainActivityViewModel
                }
                if (currentScreen is TonConnectSendRequestScreen) {
                    currentScreen.mainActivityViewModel = mainActivityViewModel
                }
                hSScreen.GetContent(backStack)
            }
        }
    )
}

fun NavBackStack<HSScreen>.paidAction(paidAction: IPaidAction, block: () -> Unit) {
    if (UserSubscriptionManager.isActionAllowed(paidAction)) {
        block.invoke()
    } else {
        val premiumFeature = PremiumFeature.getFeature(paidAction)
        add(DefenseSystemFeatureScreen(premiumFeature))
    }
}

fun NavBackStack<HSScreen>.navigateWithPaidAction(paidAction: IPaidAction, screen: HSScreen) {
    if (UserSubscriptionManager.isActionAllowed(paidAction)) {
        add(screen)
    } else {
        val premiumFeature = PremiumFeature.getFeature(paidAction)
        add(DefenseSystemFeatureScreen(premiumFeature, screen))
    }
}

fun NavBackStack<HSScreen>.navigateWithTermsAccepted(screen: HSScreen) {
    if (!App.termsManager.allTermsAccepted) {
        add(TermsScreen(screen))
    } else {
        add(screen)
    }
}

fun NavBackStack<HSScreen>.removeLastUntil(klass: KClass<out HSScreen>, inclusive: Boolean) {
    val index = indexOfLast { it::class == klass }
    if (index != -1) {
        for (i in lastIndex downTo (index + 1)) {
            removeAt(i)
        }
        if (inclusive) {
            removeAt(index)
        }
    }
}

@Composable
inline fun <reified VM : ViewModel> NavBackStack<HSScreen>.viewModelForScreen(klass: KClass<out HSScreen>) : VM {
    val hSScreen = checkNotNull(findLast { it::class == klass })

    return viewModel(
        viewModelStoreOwner = rememberChildViewModelStoreOwner(hSScreen.contentKey()),
    )
}
