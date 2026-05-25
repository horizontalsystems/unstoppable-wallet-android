package io.horizontalsystems.bankwallet.modules.nav3

import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.main.MainScreenValidationError
import io.horizontalsystems.bankwallet.modules.pin.ui.PinUnlock
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCRequestSheet
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionSheet
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.hideKeyboard
import io.horizontalsystems.dapp.core.HSDAppEvent

@Composable
fun Nav3() {
    val mainActivityViewModel = hiltViewModel<MainActivityViewModel>()
    val isLocked by App.pinComponent.isLockedFlow.collectAsState()

    val backStack = rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        NavBackStack<HSPage>(EntryPage)
    }

    val hsNavigation = remember { HSNavigation(backStack) }

    HandleNavigateToMain(mainActivityViewModel, hsNavigation)
    IntentEffect(mainActivityViewModel)
    Validate(mainActivityViewModel)
    HandleWcEvent(mainActivityViewModel, hsNavigation)
    ToggleScreenshot(hsNavigation)

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
            exit = fadeOut()
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
    navController: HSNavigation
) {
    val navigateToMain by viewModel.navigateToMainLiveData.observeAsState()
    LaunchedEffect(navigateToMain) {
        if (navigateToMain != null) {
            navController.removeLastUntil(EntryPage::class, false)
            viewModel.onNavigatedToMain()
        }
    }
}

@Composable
private fun IntentEffect(viewModel: MainActivityViewModel) {
    val activity = LocalActivity.current
    LaunchedEffect(Unit) {
        activity?.intent?.let { viewModel.setIntent(it) }
    }
    DisposableEffect(activity) {
        val consumer = Consumer<Intent> { viewModel.setIntent(it) }
        (activity as? ComponentActivity)?.addOnNewIntentListener(consumer)
        onDispose {
            (activity as? ComponentActivity)?.removeOnNewIntentListener(consumer)
        }
    }
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
    navController: HSNavigation
) {
    val view = LocalView.current
    val hudTextConnected = stringResource(R.string.Hud_Text_Connected)
    val wcEvent by viewModel.wcEvent.observeAsState()
    LaunchedEffect(wcEvent) {
        val event = wcEvent ?: return@LaunchedEffect
        when (event) {
            is HSDAppEvent.SessionRequest -> navController.slideFromBottom(WCRequestSheet)
            is HSDAppEvent.SessionProposal -> navController.slideFromBottom(WCSessionSheet(null))
            is HSDAppEvent.Error -> HudHelper.showErrorMessage(view, event.throwable.message ?: "Error")
            is HSDAppEvent.SessionSettled -> HudHelper.showSuccessMessage(view, hudTextConnected)
            else -> {}
        }
        viewModel.onWcEventHandled()
    }
}

@Composable
private fun ToggleScreenshot(navController: HSNavigation) {
    val activity = LocalActivity.current
    val currentScreen = navController.lastOrNull()
    LaunchedEffect(currentScreen) {
        if (activity != null) {
            activity.currentFocus?.hideKeyboard(activity)
            if (currentScreen?.screenshotEnabled == false) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
