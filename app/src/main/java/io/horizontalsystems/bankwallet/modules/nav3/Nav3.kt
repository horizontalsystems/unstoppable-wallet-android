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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.intro.IntroActivity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel.Factory
import io.horizontalsystems.bankwallet.modules.main.MainScreenValidationError
import io.horizontalsystems.bankwallet.modules.pin.ui.PinUnlock
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionBottomSheet
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.hideKeyboard
import io.horizontalsystems.dapp.core.HSDAppEvent
import kotlin.reflect.KClass

@Composable
fun Nav3() {
    val mainActivityViewModel = viewModel<MainActivityViewModel>(factory = Factory())
    val isLocked by App.pinComponent.isLockedFlow.collectAsState()

    val backStack = rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        NavBackStack<HSScreen>(MainScreen)
    }

    HandleNavigateToMain(mainActivityViewModel, backStack)
    IntentEffect(mainActivityViewModel)
    Validate(mainActivityViewModel)
    HandleWcEvent(mainActivityViewModel, backStack)
    ToggleScreenshot(backStack)

    val activity = LocalActivity.current

    Box {
        val eventBusNavEntryDecorator = rememberResultEventBusNavEntryDecorator<HSScreen>()
        val bottomSheetStrategy = remember { BottomSheetSceneStrategy<HSScreen>() }
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
                    screen.GetContent(backStack)
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
    backStack: NavBackStack<HSScreen>
) {
    val navigateToMain by viewModel.navigateToMainLiveData.observeAsState()
    LaunchedEffect(navigateToMain) {
        if (navigateToMain != null) {
            backStack.removeLastUntil(MainScreen::class, false)
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
        } catch (_: MainScreenValidationError.Welcome) {
            activity?.let { IntroActivity.start(it); it.finish() }
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
    backStack: NavBackStack<HSScreen>
) {
    val view = LocalView.current
    val hudTextConnected = stringResource(R.string.Hud_Text_Connected)
    val wcEvent by viewModel.wcEvent.observeAsState()
    LaunchedEffect(wcEvent) {
        val event = wcEvent ?: return@LaunchedEffect
        when (event) {
            is HSDAppEvent.SessionRequest -> backStack.slideFromBottom(WCRequestFragment())
            is HSDAppEvent.SessionProposal -> backStack.slideFromBottom(WCSessionBottomSheet(null))
            is HSDAppEvent.Error -> HudHelper.showErrorMessage(view, event.throwable.message ?: "Error")
            is HSDAppEvent.SessionSettled -> HudHelper.showSuccessMessage(view, hudTextConnected)
            else -> {}
        }
        viewModel.onWcEventHandled()
    }
}

@Composable
private fun ToggleScreenshot(backStack: NavBackStack<HSScreen>) {
    val activity = LocalActivity.current
    val currentScreen = backStack.lastOrNull()
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

@Composable
inline fun <reified VM : ViewModel> NavBackStack<HSScreen>.viewModelForPrevScreen() : VM {
    val hSScreen = this[lastIndex - 1]

    return viewModel(
        viewModelStoreOwner = rememberChildViewModelStoreOwner(hSScreen.contentKey()),
    )
}
