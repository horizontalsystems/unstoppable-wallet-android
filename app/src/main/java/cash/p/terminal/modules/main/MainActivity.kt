package cash.p.terminal.modules.main

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.compose.runtime.LaunchedEffect
import cash.p.terminal.MainGraphDirections
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseActivity
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.notifications.TransactionNotificationManager
import cash.p.terminal.core.navigateWithTermsAccepted
import cash.p.terminal.modules.createaccount.CreateAccountFragment
import cash.p.terminal.modules.intro.IntroActivity
import cash.p.terminal.modules.keystore.KeyStoreActivity
import cash.p.terminal.modules.calculator.lockscreen.CalculatorLockScreen
import cash.p.terminal.modules.calculator.lockscreen.CalculatorLockScreenActions
import cash.p.terminal.modules.calculator.lockscreen.CalculatorLockScreenViewModel
import cash.p.terminal.modules.pin.ui.PinUnlock
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import cash.p.terminal.modules.tonconnect.TonConnectNewFragment
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.navigation.slideFromBottomForResult
import cash.p.terminal.navigation.slideFromRightClearingBackStack
import cash.p.terminal.tangem.domain.sdk.CardSdkProvider
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import com.reown.walletkit.client.Wallet
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.hideKeyboard
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

open class MainActivity : BaseActivity() {

    val viewModel: MainActivityViewModel by inject()
    private val cardSdkProvider: CardSdkProvider by inject()
    private val localStorage: ILocalStorage by inject()
    private val pinComponent: IPinComponent by inject()
    private var pinLockComposeView: ComposeView? = null
    private var showPinLockScreen by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        // Refresh lock state synchronously: BackgroundManager → PinComponent emits
        // EnterForeground on a different coroutine and may not have run yet, so the
        // check below would otherwise race and briefly hide the calculator overlay.
        pinComponent.willEnterForeground()
        if (showPinLockScreen && !pinComponent.isLockedFlow.value) {
            showPinLockScreen = false
            pinLockComposeView?.visibility = GONE
            applyLockWindowFlags(isLocked = false, calculatorMode = true)
        }
        validate()
    }

    override fun onPause() {
        super.onPause()
        showCalculatorLockScreenInRecents()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isDeepLink = intent.data != null && intent.action == Intent.ACTION_VIEW
        val isNotificationTap = intent.hasExtra(TransactionNotificationManager.EXTRA_RECORD_UID)
        if (isDeepLink || isNotificationTap) {
            val navHost =
                supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
            val navController = navHost.navController
            navController.popBackStack(navController.graph.startDestinationId, false)
        }
        viewModel.setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // If SQLCipher failed, BaseActivity redirected to error screen - don't continue
        if (App.sqlCipherLoadFailed) return

        cardSdkProvider.register(this)
        applyTaskDescription(localStorage.isCalculatorModeEnabled)

        setContentView(R.layout.activity_main)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHost.navController

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
                when (wcEvent) {
                    is Wallet.Model.SessionRequest -> {
                        navController.slideFromBottom(R.id.wcRequestFragment)
                    }

                    is Wallet.Model.SessionProposal -> {
                        navController.slideFromBottom(
                            MainGraphDirections.actionGlobalToWcSessionFragment(null)
                        )
                    }

                    else -> {}
                }

                viewModel.onWcEventHandled()
            }
        }

        lifecycleScope.launch {
            viewModel.tcSendRequest.collect { tcEvent ->
                if (tcEvent != null) {
                    navController.slideFromBottom(R.id.tcSendRequestFragment)
                }
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

        // Handle deeplink or notification tap on cold start (only on fresh launch, not on recreation)
        if (savedInstanceState == null) {
            val isDeepLink = intent.data != null && intent.action == Intent.ACTION_VIEW
            val isNotificationTap = intent.hasExtra(TransactionNotificationManager.EXTRA_RECORD_UID)
            if (isDeepLink || isNotificationTap) {
                viewModel.setIntent(intent)
            }
        }

        val composeView = findViewById<ComposeView>(R.id.pinLockComposeView)
        pinLockComposeView = composeView
        composeView.setContent {
            ComposeAppTheme {
                val calculatorMode by localStorage.isCalculatorModeEnabledFlow
                    .collectAsStateWithLifecycle()
                if (calculatorMode) {
                    val viewModel: CalculatorLockScreenViewModel = koinViewModel()
                    val state = viewModel.uiState
                    LaunchedEffect(state.unlocked) {
                        if (state.unlocked) {
                            showPinLockScreen = false
                            viewModel.onUnlockedConsumed()
                        }
                    }
                    if (showPinLockScreen) {
                        CalculatorLockScreen(
                            uiState = state,
                            actions = CalculatorLockScreenActions(
                                onDigit = viewModel::onDigitClick,
                                onOperator = viewModel::onOperatorClick,
                                onDecimal = viewModel::onDecimalClick,
                                onParen = viewModel::onParenClick,
                                onToggleSign = viewModel::onToggleSignClick,
                                onDelete = viewModel::onDeleteClick,
                                onClear = viewModel::onClearClick,
                                onEquals = viewModel::onEqualsClick,
                            ),
                        )
                    }
                } else {
                    PinUnlock(
                        showPinLockScreen = showPinLockScreen,
                        onSuccess = {
                            showPinLockScreen = false
                        }
                    )
                }
            }
        }
        observeLockState()
    }

    private fun closeAfterDelay() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 1000)
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

    private fun observeLockState() {
        lifecycleScope.launch {
            combine(
                viewModel.isLockedFlow,
                localStorage.isCalculatorModeEnabledFlow,
            ) { locked, calculatorMode -> locked to calculatorMode }
                .collect { (isLocked, calculatorMode) ->
                    showPinLockScreen = isLocked
                    pinLockComposeView?.visibility = if (isLocked) VISIBLE else GONE
                    applyTaskDescription(calculatorMode)
                    applyLockWindowFlags(isLocked, calculatorMode)
                    if (isLocked) {
                        dismissOpenDialogFragments()
                    }
                }
        }
    }

    // BottomSheet/DialogFragments live in their own Window — they render above the
    // in-activity lock/calculator screen and would leak wallet UI through the disguise.
    private fun dismissOpenDialogFragments() {
        collectDialogFragments(supportFragmentManager).forEach {
            it.dismissAllowingStateLoss()
        }
    }

    private fun collectDialogFragments(fm: FragmentManager): List<DialogFragment> {
        val result = mutableListOf<DialogFragment>()
        fm.fragments.forEach { fragment ->
            if (fragment is DialogFragment) {
                result += fragment
            }
            if (fragment != null && fragment.isAdded) {
                result += collectDialogFragments(fragment.childFragmentManager)
            }
        }
        return result
    }

    private fun showCalculatorLockScreenInRecents() {
        val composeView = pinLockComposeView ?: return
        if (!localStorage.isCalculatorModeEnabled || !pinComponent.isPinSet) {
            return
        }

        showPinLockScreen = true
        composeView.visibility = VISIBLE
        applyTaskDescription(calculatorMode = true)
        applyLockWindowFlags(isLocked = true, calculatorMode = true)
    }

    private fun applyLockWindowFlags(isLocked: Boolean, calculatorMode: Boolean) {
        if (isLocked) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            if (calculatorMode) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun applyTaskDescription(calculatorMode: Boolean) {
        val labelRes = if (calculatorMode) R.string.calculator_app_name else R.string.App_Name
        setTitle(labelRes)
        setTaskDescription(ActivityManager.TaskDescription(getString(labelRes)))
    }

    private fun findNavController(): NavController {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        return navHost.navController
    }

    fun openCreateNewWallet() {
        viewModel.selectBalanceTabOnNextLaunch()
        // Set flag to select Balance tab when returning to main screen
        // Open create wallet screen after PIN is created, clearing back stack to main screen
        findNavController().navigateWithTermsAccepted {
            findNavController().slideFromRightClearingBackStack(
                resId = R.id.createAccountFragment,
                popUpToId = R.id.mainFragment,
                input = CreateAccountFragment.Input(
                    popOffOnSuccess = R.id.mainFragment,
                    popOffInclusive = false
                )
            )
        }
    }
}
