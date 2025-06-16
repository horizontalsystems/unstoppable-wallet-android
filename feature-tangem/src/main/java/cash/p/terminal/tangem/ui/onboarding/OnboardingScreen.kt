package cash.p.terminal.tangem.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import cash.p.terminal.tangem.R
import cash.p.terminal.tangem.ui.HardwareWalletError
import cash.p.terminal.tangem.ui.HardwareWalletOnboardingFragment
import cash.p.terminal.tangem.ui.HardwareWalletOnboardingViewModel
import cash.p.terminal.tangem.ui.OnboardingStep
import cash.p.terminal.tangem.ui.accesscode.AddAccessCodeDialog
import cash.p.terminal.tangem.ui.resetBackupDialog.ResetBackupDialog
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResultX
import io.horizontalsystems.core.slideFromBottomForResult

@Composable
internal fun OnboardingScreen(
    viewModel: HardwareWalletOnboardingViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState.value
    val view = LocalView.current
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.create_wallet),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        }
    ) { paddingValues ->

        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorEvents.collect { error ->
                    val resId = when (error) {
                        HardwareWalletError.UnknownError -> {
                            R.string.error_wallets_creating
                        }

                        HardwareWalletError.WalletsNotCreated -> {
                            R.string.error_wallets_creating
                        }

                        HardwareWalletError.AttestationFailed -> {
                            R.string.issuer_signature_loading_failed
                        }

                        HardwareWalletError.ErrorInBackupCard -> {
                            R.string.error_backup_card
                        }

                        HardwareWalletError.CardNotActivated -> null
                        is HardwareWalletError.NeedFactoryReset -> {
                            navController.slideFromBottomForResult<ResetBackupDialog.Result>(
                                R.id.resetBackupDialog
                            ) {
                                if (it.confirmed) {
                                    viewModel.resetCard(error.cardId)
                                }
                            }
                            null
                        }
                    }
                    if (resId != null) {
                        HudHelper.showErrorMessage(
                            contenView = view,
                            resId = resId
                        )
                    }
                }
            }
        }

        LaunchedEffect(uiState.success) {
            if(uiState.success) {
                navController.setNavigationResultX(HardwareWalletOnboardingFragment.Result(true))
                navController.popBackStack()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { uiState.currentStep.progress },
                color = ComposeAppTheme.colors.yellowD,
                trackColor = ComposeAppTheme.colors.steel20,
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "onboarding_animation"
            ) { step ->
                when (step) {
                    OnboardingStep.CREATE_WALLET -> CreateWalletScreen(
                        onCreateWalletClick = viewModel::createWallet
                    )

                    OnboardingStep.ADD_BACKUP -> CreateBackupScreen(
                        onCreateBackupClick = viewModel::createBackup,
                        onGoToFinalPageClick = viewModel::onGoToFinalPageClick,
                        uiState = viewModel.uiState.value
                    )

                    OnboardingStep.CREATE_ACCESS_CODE -> CreateAccessCodeScreen(
                        onCreateCodeClick = {
                            navController.slideFromBottomForResult<AddAccessCodeDialog.Result>(R.id.addAccessCodeDialog) {
                                if (it.code.isNotEmpty()) {
                                    viewModel.setAccessCode(it.code)
                                }
                            }
                        }
                    )

                    OnboardingStep.FINAL -> FinalScreen(
                        uiState = viewModel.uiState.value,
                        onWriteDataClicked = viewModel::onWriteFinalDataClicked
                    )
                }
            }
        }
    }
}