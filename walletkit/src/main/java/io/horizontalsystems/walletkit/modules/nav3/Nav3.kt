package io.horizontalsystems.walletkit.modules.nav3

import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.util.Consumer
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.hideKeyboard
import io.horizontalsystems.walletkit.modules.keystore.KeyStoreActivity
import io.horizontalsystems.walletkit.modules.main.MainActivityViewModel
import io.horizontalsystems.walletkit.modules.main.MainActivityViewModel.Factory
import io.horizontalsystems.walletkit.modules.main.MainScreenValidationError
import io.horizontalsystems.walletkit.modules.pin.ui.PinUnlock
import io.horizontalsystems.walletkit.modules.walletconnect.WCAccountTypeNotSupportedSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCErrorNoAccountSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager
import io.horizontalsystems.walletkit.modules.walletconnect.request.WCRequestSheet
import io.horizontalsystems.walletkit.modules.walletconnect.session.WCSessionSheet
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.dapp.core.HSDAppEvent

@Composable
fun Nav3(entryPage: HSPage) {
    val mainActivityViewModel = viewModel<MainActivityViewModel>(factory = Factory())
    val isLocked by App.pinComponent.isLockedFlow.collectAsState()

    val backStack = rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        NavBackStack<HSPage>(entryPage)
    }

    val hsNavigation = remember { HSNavigation(backStack) }

    HandleNavigateToMain(mainActivityViewModel, hsNavigation)
    IntentEffect(mainActivityViewModel, hsNavigation)
    Validate(mainActivityViewModel)
    HandleWcEvent(mainActivityViewModel, hsNavigation)
    ToggleScreenshot(hsNavigation, isLocked)

    LaunchedEffect(isLocked) {
        if (!isLocked) {
            // Pair with any `wc:` link that arrived while locked, then re-show any WC
            // request/proposal that arrived while locked
            App.wcManager.flushPendingPairing()
            mainActivityViewModel.reEmitPendingWcEventIfNeeded()
        }
    }

    val activity = LocalActivity.current

    Box {
        val eventBusNavEntryDecorator = rememberResultEventBusNavEntryDecorator<HSPage>()
        val bottomSheetStrategy = remember { BottomSheetSceneStrategy<HSPage>() }
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberSharedViewModelStoreNavEntryDecorator(),
                eventBusNavEntryDecorator,
            ),
            backStack = backStack,
            // Guarded pop: NavDisplay's default onBack removes entries without
            // protecting the root, so a racing back event can empty the stack.
            onBack = hsNavigation::removeLastOrNull,
            sceneStrategies = listOf(bottomSheetStrategy),
            entryProvider = { screen ->
                eventBusNavEntryDecorator.setResultKey(screen.resultKey)
                NavEntry(
                    key = screen,
                    contentKey = screen.contentKey(),
                    metadata = screen.getMetadata()
                ) {
                    screen.GetContent(hsNavigation)
                }
            }
        )

        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(),
            // Leaves immediately rather than fading. The secure flag is cleared as soon as the app
            // unlocks, so an exit animation would keep the keypad composited on a surface that is
            // capturable again. Entering is unaffected: the flag is set before the fade in starts.
            exit = ExitTransition.None
        ) {
            PinUnlock(isLocked = isLocked)
        }
    }

    BackHandler(enabled = isLocked) {
        activity?.moveTaskToBack(true)
    }
}

@Composable
private fun HandleNavigateToMain(
    viewModel: MainActivityViewModel,
    navigation: HSNavigation
) {
    val navigateToMain by viewModel.navigateToMainLiveData.observeAsState()
    LaunchedEffect(navigateToMain) {
        if (navigateToMain != null) {
            navigation.removeLastUntil(EntryPage::class, false)
            viewModel.onNavigatedToMain()
        }
    }
}

@Composable
private fun IntentEffect(viewModel: MainActivityViewModel, navigation: HSNavigation) {
    val activity = LocalActivity.current
    LaunchedEffect(Unit) {
        activity?.intent?.let {
            if (!handleWalletConnectDeepLink(it, navigation)) {
                viewModel.setIntent(it)
            }
        }
    }
    DisposableEffect(activity) {
        val consumer = Consumer<Intent> {
            if (!handleWalletConnectDeepLink(it, navigation)) {
                viewModel.setIntent(it)
            }
        }
        (activity as? ComponentActivity)?.addOnNewIntentListener(consumer)
        onDispose {
            (activity as? ComponentActivity)?.removeOnNewIntentListener(consumer)
        }
    }
}

// WalletConnect deeplinks are handled here, at the navigation root, so they work no matter which
// inner screen is shown (the deeplink -> handleDeepLink flow in MainScreen only runs while
// MainScreen is composed, so a `wc:` link received on e.g. the WC list page would otherwise be
// ignored). Pairing here is enough — the connect proposal dialog is opened by HandleWcEvent
// regardless of the current destination.
//
// Returns true if the intent was a WalletConnect deeplink and was handled here. When DAppManager
// isn't available yet (e.g. cold start before the relay connects) it returns false so the intent
// falls back to MainScreen's deeplink flow, which waits and routes through the WC list.
private fun handleWalletConnectDeepLink(intent: Intent, navigation: HSNavigation): Boolean {
    val uri = intent.data ?: return false
    val wcUri = App.wcManager.getWalletConnectUri(uri) ?: return false
    if (!DAppManager.isAvailable) return false

    when (val supportState = App.wcManager.getWalletConnectSupportState()) {
        WCManager.SupportState.Supported -> {
            // Held until unlock while the app is locked — see WCManager.pairOrDefer.
            App.wcManager.pairOrDefer(wcUri)
        }

        WCManager.SupportState.NotSupportedDueToNoActiveAccount -> {
            navigation.slideFromBottom(WCErrorNoAccountSheet)
        }

        is WCManager.SupportState.NotSupported -> {
            navigation.slideFromBottom(
                WCAccountTypeNotSupportedSheet(
                    WCAccountTypeNotSupportedSheet.Input(supportState.accountTypeDescription)
                )
            )
        }
    }
    return true
}

@Composable
private fun Validate(viewModel: MainActivityViewModel) {
    val activity = LocalActivity.current
    LifecycleResumeEffect(Unit) {
        try {
            viewModel.validate()
        } catch (_: MainScreenValidationError.NoSystemLock) {
            activity?.let { KeyStoreActivity.startForNoSystemLock(it); it.finish() }
        } catch (_: MainScreenValidationError.KeyInvalidated) {
            activity?.let { KeyStoreActivity.startForInvalidKey(it); it.finish() }
        } catch (_: MainScreenValidationError.UserAuthentication) {
            activity?.let { KeyStoreActivity.startForUserAuthentication(it); it.finish() }
        } catch (_: MainScreenValidationError.KeystoreRuntimeException) {
            Toast.makeText(App.instance, "Issue with Keystore", Toast.LENGTH_SHORT).show()
            activity?.finish()
        }
        onPauseOrDispose {}
    }
}

@Composable
private fun HandleWcEvent(
    viewModel: MainActivityViewModel,
    navigation: HSNavigation
) {
    val view = LocalView.current
    val hudTextConnected = stringResource(R.string.Hud_Text_Connected)
    val wcEvent by viewModel.wcEvent.observeAsState()
    LaunchedEffect(wcEvent) {
        val event = wcEvent ?: return@LaunchedEffect

        // Don't open WC bottom sheets while the app is locked. The event is retained in
        // WCDelegate and re-emitted via reEmitPendingWcEventIfNeeded() once the user unlocks,
        // so the request/proposal isn't lost behind the pin screen.
        val deferWhileLocked = App.pinComponent.isLocked &&
                (event is HSDAppEvent.SessionRequest || event is HSDAppEvent.SessionProposal)
        if (deferWhileLocked) {
            viewModel.onWcEventHandled()
            return@LaunchedEffect
        }

        when (event) {
            is HSDAppEvent.SessionRequest -> {
                // reEmitPendingWcEventIfNeeded() can fire more than once per foreground
                // transition (from both the unlock effect and MainScreen's ON_RESUME), so skip
                // if the request sheet is already shown to avoid stacking duplicates.
                if (navigation.lastOrNull() !is WCRequestSheet) {
                    navigation.slideFromBottom(WCRequestSheet)
                }
            }

            is HSDAppEvent.SessionProposal -> {
                if (navigation.lastOrNull() !is WCSessionSheet) {
                    navigation.slideFromBottom(WCSessionSheet(null))
                }
            }

            is HSDAppEvent.Error -> HudHelper.showErrorMessage(view, event.throwable.message ?: "Error")
            is HSDAppEvent.SessionSettled -> HudHelper.showSuccessMessage(view, hudTextConnected)
            else -> {}
        }
        viewModel.onWcEventHandled()
    }
}

@Composable
private fun ToggleScreenshot(navigation: HSNavigation, isLocked: Boolean) {
    val activity = LocalActivity.current
    val currentScreen = navigation.lastOrNull()
    LaunchedEffect(currentScreen, isLocked) {
        if (activity != null) {
            activity.currentFocus?.hideKeyboard(activity)
            // The unlock keypad is drawn as an overlay rather than pushed as a page, so it has no
            // screenshotEnabled of its own. Without the lock state here the flag stays however the
            // page underneath left it, and locking over an ordinary screen leaves passcode entry
            // recordable.
            if (isLocked || currentScreen?.screenshotEnabled == false) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
