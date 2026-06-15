package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.modules.intro.IntroActivity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.pin.ui.PinUnlock
import io.horizontalsystems.bankwallet.modules.tonconnect.TonConnectNewFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.hideKeyboard
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.dapp.core.HSDAppEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var pinLockComposeView: ComposeView
    private lateinit var navController: NavController
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    private var showPinLockScreen by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        validate()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!handleWalletConnectDeepLink(intent)) {
            viewModel.setIntent(intent)
        }
    }

    // WalletConnect deeplinks are handled here, at the activity level, so they work no matter which
    // inner screen is shown (the deeplink -> handleDeepLink flow in MainFragment only runs while
    // MainFragment is active, so a `wc:` link received on e.g. the WC list page would otherwise be
    // ignored until that page is closed). Pairing here is enough — the connect proposal dialog is
    // opened by the activity-level wcEvent observer regardless of the current destination.
    //
    // Returns true if the intent was a WalletConnect deeplink and was handled here. When DAppManager
    // isn't available yet (e.g. cold start before the relay connects) it returns false so the intent
    // falls back to MainFragment's deeplink flow, which waits and routes through the WC list.
    private fun handleWalletConnectDeepLink(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        val wcUri = App.wcManager.getWalletConnectUri(uri) ?: return false
        if (!DAppManager.isAvailable) return false

        when (val supportState = App.wcManager.getWalletConnectSupportState()) {
            WCManager.SupportState.Supported -> {
                DAppManager.pair(wcUri.trim())
            }

            WCManager.SupportState.NotSupportedDueToNoActiveAccount -> {
                navController.slideFromBottom(R.id.wcErrorNoAccountFragment)
            }

            is WCManager.SupportState.NotSupported -> {
                navController.slideFromBottom(
                    R.id.wcAccountTypeNotSupportedDialog,
                    WCAccountTypeNotSupportedDialog.Input(supportState.accountTypeDescription)
                )
            }
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        pinLockComposeView = findViewById(R.id.pinLockComposeView)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHost.navController

        navController.setGraph(R.navigation.main_graph, intent.extras)
        navController.addOnDestinationChangedListener { _, _, _ ->
            currentFocus?.hideKeyboard(this)
        }

        viewModel.navigateToMainLiveData.observe(this) {
            if (it) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                viewModel.onNavigatedToMain()
            }
        }

        viewModel.wcEvent.observe(this) { wcEvent ->
            if (wcEvent != null) {
                // Don't open WC bottom sheets while the app is locked. The event is
                // retained in WCDelegate and re-emitted in observeLockState() once the
                // user unlocks, so the request/proposal isn't lost behind the pin screen.
                val deferWhileLocked = App.pinComponent.isLocked &&
                        (wcEvent is HSDAppEvent.SessionRequest || wcEvent is HSDAppEvent.SessionProposal)
                if (deferWhileLocked) {
                    viewModel.onWcEventHandled()
                    return@observe
                }
                when (wcEvent) {
                    is HSDAppEvent.SessionRequest -> {
                        // reEmitPendingWcEventIfNeeded() can fire more than once per foreground
                        // transition (from both observeLockState and MainFragment's ON_RESUME),
                        // so skip if the request dialog is already shown to avoid stacking duplicates.
                        if (navController.currentDestination?.id != R.id.wcRequestFragment) {
                            navController.slideFromBottom(R.id.wcRequestFragment)
                        }
                    }

                    is HSDAppEvent.SessionProposal -> {
                        if (navController.currentDestination?.id != R.id.wcSessionBottomSheetDialog) {
                            navController.slideFromBottom(R.id.wcSessionBottomSheetDialog)
                        }
                    }

                    is HSDAppEvent.Error -> {
                        navHost.view?.let {
                            HudHelper.showErrorMessage(it, wcEvent.throwable.message ?: "Error")
                        }
                    }

                    is HSDAppEvent.SessionSettled -> {
                        navHost.view?.let {
                            HudHelper.showSuccessMessage(it, getString(R.string.Hud_Text_Connected))
                        }
                    }

                    else -> {}
                }

                viewModel.onWcEventHandled()
            }
        }

        viewModel.tcSendRequest.observe(this) { request ->
            if (request != null) {
                navController.slideFromBottom(R.id.tcSendRequestFragment)
            }
        }

        viewModel.tcDappRequest.observe(this) { request ->
            if (request != null) {
                navController.slideFromBottomForResult<TonConnectNewFragment.Result>(
                    R.id.tcNewFragment,
                    request.dAppRequest
                ) { result ->
                    if (request.closeAppOnResult) {
                        if (result.approved) {
                            //Need delay to get connected before closing activity
                            closeAfterDelay()
                        } else {
                            finish()
                        }
                    }
                }
                viewModel.onTcDappRequestHandled()
            }
        }

        if (!handleWalletConnectDeepLink(intent)) {
            viewModel.setIntent(intent)
        }

        pinLockComposeView.setContent {
            ComposeAppTheme {
                PinUnlock(
                    showPinLockScreen = showPinLockScreen,
                    onSuccess = {
                        showPinLockScreen = false
                    }
                )
            }
        }

        observeLockState()
    }

    private fun observeLockState() {
        scope.launch {
            App.pinComponent.isLockedFlow.collect { isLocked ->
                showPinLockScreen = isLocked
                pinLockComposeView.visibility = if (isLocked) { VISIBLE } else { GONE }

                if (!isLocked) {
                    // Re-show any WC request/proposal that arrived while locked
                    viewModel.reEmitPendingWcEventIfNeeded()
                }
            }
        }
    }

    private fun closeAfterDelay() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            finish()
        }, 1000)
    }

    private fun validate() = try {
        viewModel.validate()
    } catch (e: MainScreenValidationError.NoSystemLock) {
        KeyStoreActivity.startForNoSystemLock(this)
        finish()
    } catch (e: MainScreenValidationError.KeyInvalidated) {
        KeyStoreActivity.startForInvalidKey(this)
        finish()
    } catch (e: MainScreenValidationError.UserAuthentication) {
        KeyStoreActivity.startForUserAuthentication(this)
        finish()
    } catch (e: MainScreenValidationError.Welcome) {
        IntroActivity.start(this)
        finish()
    } catch (e: MainScreenValidationError.KeystoreRuntimeException) {
        Toast.makeText(App.instance, "Issue with Keystore", Toast.LENGTH_SHORT).show()
        finish()
    }
}
