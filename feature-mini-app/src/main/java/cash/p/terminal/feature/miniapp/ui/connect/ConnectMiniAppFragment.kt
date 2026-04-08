package cash.p.terminal.feature.miniapp.ui.connect

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.findNavController
import cash.p.terminal.feature.miniapp.R
import cash.p.terminal.feature.miniapp.ui.TELEGRAM_BOT_URL
import cash.p.terminal.feature.miniapp.ui.components.rememberStepIndicatorState
import cash.p.terminal.feature.miniapp.ui.connect.screens.AcceptTermsStepScreen
import cash.p.terminal.feature.miniapp.ui.connect.screens.CaptchaStepScreen
import cash.p.terminal.feature.miniapp.ui.connect.screens.CreateWalletStepScreen
import cash.p.terminal.feature.miniapp.ui.connect.screens.FinishStepScreen
import cash.p.terminal.feature.miniapp.ui.connect.screens.SpecialProposalStepScreen
import cash.p.terminal.feature.miniapp.ui.connect.screens.SpecialProposalUiState
import cash.p.terminal.feature.miniapp.ui.connect.screens.TokenCheckingScreen
import cash.p.terminal.feature.miniapp.ui.connect.screens.TokenMissingScreen
import cash.p.terminal.feature.miniapp.ui.connect.screens.WalletSelectionScreen
import cash.p.terminal.navigation.BackupKeyInput
import cash.p.terminal.navigation.entity.SwapParams
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.IAccountManager
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.inject

class ConnectMiniAppFragment : BaseComposeFragment() {

    private val viewModel by viewModel<ConnectMiniAppViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        val internalNavController = rememberNavController()

        ConnectMiniAppNavHost(
            viewModel = viewModel,
            fragmentNavController = navController,
            navController = internalNavController,
            onOpenMiniAppClick = ::openMiniApp
        )
    }

    private fun openMiniApp() {
        val context = requireContext()
        findNavController().popBackStack()
        val intent = Intent(Intent.ACTION_VIEW, TELEGRAM_BOT_URL.toUri())
        context.startActivity(intent)
    }
}

@Serializable
private sealed class ConnectMiniAppRoute {
    @Serializable
    data object CreateWallet : ConnectMiniAppRoute()
}

@Composable
private fun ConnectMiniAppNavHost(
    viewModel: ConnectMiniAppViewModel,
    fragmentNavController: NavController,
    navController: NavHostController,
    onOpenMiniAppClick: () -> Unit
) {
    val uiState = viewModel.uiState

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.connect_mini_app_title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.PlainString(""),
                        icon = R.drawable.ic_close_24,
                        onClick = { fragmentNavController.popBackStack() }
                    )
                )
            )
        }
    ) { paddingValues ->
        val stepIndicatorState = rememberStepIndicatorState()
        val coroutineScope = rememberCoroutineScope()

        // Update state when step changes
        LaunchedEffect(uiState.currentStep) {
            stepIndicatorState.currentStep = uiState.currentStep
        }

        NavHost(
            navController = navController,
            startDestination = ConnectMiniAppRoute.CreateWallet
        ) {
            composable<ConnectMiniAppRoute.CreateWallet> {
                when (uiState.currentStep) {
                    ConnectMiniAppViewModel.STEP_WALLET -> {
                        when {
                            uiState.isCheckingTokens -> {
                                TokenCheckingScreen(
                                    stepIndicatorState = stepIndicatorState,
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }

                            uiState.missingTokenNames.isNotEmpty() -> {
                                TokenMissingScreen(
                                    allTokensText = uiState.allTokensText,
                                    missingTokenNames = uiState.missingTokenNames,
                                    isAddingTokens = uiState.isAddingTokens,
                                    onAddClick = viewModel::onAddTokensClick,
                                    stepIndicatorState = stepIndicatorState,
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }

                            uiState.walletItems.size > 1 && uiState.chosenAccountId == null -> {
                                WalletSelectionScreen(
                                    isLoading = uiState.isLoading,
                                    walletItems = uiState.walletItems,
                                    selectedAccountId = uiState.preselectedAccountId,
                                    onWalletSelected = viewModel::onWalletSelected,
                                    onContinueClick = viewModel::onConfirmWalletSelectedClick,
                                    stepIndicatorState = stepIndicatorState,
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }

                            else -> {
                                CreateWalletStepScreen(
                                    isLoading = uiState.isLoading,
                                    needsBackup = uiState.needsBackup,
                                    onNewWalletClick = {
                                        fragmentNavController.slideFromRight(R.id.createAccountFragment)
                                    },
                                    onImportWalletClick = {
                                        fragmentNavController.slideFromRight(R.id.restoreAccountFragment)
                                    },
                                    onManualBackupClick = {
                                        uiState.chosenAccountId?.let { chosenAccountId ->
                                            fragmentNavController.slideFromBottom(
                                                R.id.backupKeyFragment,
                                                BackupKeyInput(chosenAccountId)
                                            )
                                        }
                                    },
                                    onLocalBackupClick = {
                                        uiState.chosenAccountId?.let { chosenAccountId ->
                                            val accountManager: IAccountManager by inject(
                                                IAccountManager::class.java
                                            )
                                            accountManager.account(chosenAccountId)
                                                ?.let { account ->
                                                    fragmentNavController.slideFromBottom(
                                                        R.id.backupLocalFragment,
                                                        account
                                                    )
                                                }
                                        }
                                    },
                                    stepIndicatorState = stepIndicatorState,
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }
                        }
                    }

                    ConnectMiniAppViewModel.STEP_TERMS -> {
                        AcceptTermsStepScreen(
                            isAgreed = uiState.termsAgreed,
                            onAgreedChange = viewModel::onTermsAgreedChange,
                            onContinueClick = viewModel::onTermsAccepted,
                            modifier = Modifier.padding(paddingValues),
                            stepIndicatorState = stepIndicatorState
                        )
                    }

                    ConnectMiniAppViewModel.STEP_CAPTCHA -> {
                        CaptchaStepScreen(
                            captchaImageBase64 = uiState.captchaImageBase64,
                            expiresInSeconds = uiState.captchaExpiresIn,
                            code = uiState.captchaCode,
                            error = uiState.captchaError,
                            isLoading = uiState.isCaptchaLoading,
                            isVerifying = uiState.isCaptchaVerifying,
                            isJwtExpired = uiState.isJwtExpired,
                            onCodeChange = viewModel::onCaptchaCodeChange,
                            onRefreshClick = viewModel::refreshCaptcha,
                            onVerifyClick = viewModel::verifyCaptcha,
                            onOpenMiniAppClick = onOpenMiniAppClick,
                            stepIndicatorState = stepIndicatorState,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }

                    ConnectMiniAppViewModel.STEP_SPECIAL_PROPOSAL -> {
                        // Refresh data when returning from buy screen
                        LifecycleResumeEffect(Unit) {
                            viewModel.loadSpecialProposalData()
                            onPauseOrDispose { }
                        }

                        SpecialProposalStepScreen(
                            uiState = SpecialProposalUiState(
                                data = uiState.specialProposalData,
                                selectedTab = uiState.selectedCoinTab,
                                isLoading = uiState.isSpecialProposalLoading,
                                isPremium = uiState.isPremiumUser,
                                error = uiState.specialProposalError,
                                isJwtExpired = uiState.isJwtExpired
                            ),
                            stepIndicatorState = stepIndicatorState,
                            onTabSelected = viewModel::onCoinTabSelected,
                            onBuyClick = {
                                coroutineScope.launch {
                                    viewModel.getTokenForSwap()?.let { token ->
                                        fragmentNavController.slideFromRight(
                                            R.id.multiswap,
                                            SwapParams.TOKEN_OUT to token
                                        )
                                    }
                                }
                            },
                            onConnectClick = viewModel::connectWallet,
                            onRetryClick = viewModel::loadSpecialProposalData,
                            onOpenMiniAppClick = onOpenMiniAppClick,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }

                    ConnectMiniAppViewModel.STEP_FINISH -> {
                        uiState.finishState?.let { finishState ->
                            // Handle close event
                            LaunchedEffect(uiState.closeEvent) {
                                if (uiState.closeEvent) {
                                    fragmentNavController.popBackStack()
                                }
                            }

                            FinishStepScreen(
                                finishState = finishState,
                                onCloseClick = viewModel::onFinishClose,
                                onRetryClick = viewModel::onRetryClick,
                                onOpenMiniAppClick = onOpenMiniAppClick,
                                stepIndicatorState = stepIndicatorState,
                                modifier = Modifier.padding(paddingValues)
                            )
                        }
                    }
                }
            }
        }
    }
}
