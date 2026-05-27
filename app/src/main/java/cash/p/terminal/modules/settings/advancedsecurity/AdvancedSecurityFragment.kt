package cash.p.terminal.modules.settings.advancedsecurity

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.authorizedDeleteContactsPasscodeAction
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.ensurePinSet
import cash.p.terminal.core.fullRestart
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.premiumAction
import cash.p.terminal.core.slideToDeleteContactsTerms
import cash.p.terminal.modules.calculator.autolock.CalculatorAutoLockScreen
import cash.p.terminal.modules.calculator.autolock.CalculatorAutoLockViewModel
import cash.p.terminal.modules.calculator.domain.CalculatorModeService
import cash.p.terminal.modules.calculator.pinsettings.CalculatorPinSettingsScreen
import cash.p.terminal.modules.calculator.pinsettings.CalculatorPinSettingsViewModel
import cash.p.terminal.modules.settings.advancedsecurity.securereset.SecureResetTermsScreen
import cash.p.terminal.modules.settings.advancedsecurity.securereset.SecureResetTermsViewModel
import cash.p.terminal.modules.settings.advancedsecurity.terms.HiddenWalletTermsScreen
import cash.p.terminal.modules.settings.advancedsecurity.terms.HiddenWalletTermsViewModel
import cash.p.terminal.navigation.navigateSafely
import cash.p.terminal.navigation.navigateUpSafely
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.IPinComponent
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class AdvancedSecurityFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AdvancedSecurityNavHost(navController)
    }
}

@Composable
private fun AdvancedSecurityNavHost(fragmentNavController: NavController) {
    val navController = rememberNavController()
    val viewModel: AdvancedSecurityViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = AdvancedSecurityRoutes.ADVANCED_SECURITY_PAGE
    ) {
        composable(AdvancedSecurityRoutes.ADVANCED_SECURITY_PAGE) {
            AdvancedSecurityScreen(
                uiState = viewModel.uiState,
                onCreateHiddenWalletClick = {
                    fragmentNavController.premiumAction {
                        navController.navigateSafely(AdvancedSecurityRoutes.HIDDEN_WALLET_TERM_SPAGE)
                    }
                },
                onSecureResetToggle = { enabled ->
                    if (enabled) {
                        fragmentNavController.premiumAction {
                            navController.navigateSafely(AdvancedSecurityRoutes.SECURE_RESET_TERMS_PAGE)
                        }
                    } else {
                        fragmentNavController.authorizedAction {
                            viewModel.onSecureResetDisabled()
                        }
                    }
                },
                onDeleteContactsToggle = { enabled ->
                    if (enabled) {
                        fragmentNavController.premiumAction {
                            fragmentNavController.slideToDeleteContactsTerms()
                        }
                    } else {
                        fragmentNavController.authorizedDeleteContactsPasscodeAction {
                            viewModel.onDeleteContactsDisabled()
                        }
                    }
                },
                onCalculatorPinClick = {
                    navController.navigate(AdvancedSecurityRoutes.CALCULATOR_PIN_PAGE)
                },
                onClose = fragmentNavController::navigateUpSafely
            )
        }
        composablePage(AdvancedSecurityRoutes.HIDDEN_WALLET_TERM_SPAGE) {
            val context = LocalContext.current
            val termTitles =
                context.resources.getStringArray(R.array.AdvancedSecurity_Terms_Checkboxes)

            val viewModel: HiddenWalletTermsViewModel = koinViewModel {
                parametersOf(termTitles)
            }
            val uiState = viewModel.uiState

            HiddenWalletTermsScreen(
                uiState = uiState,
                onCheckboxToggle = viewModel::toggleCheckbox,
                onAgreeClick = {
                    fragmentNavController.ensurePinSet(R.string.PinSet_Title) {
                        fragmentNavController.slideFromRight(R.id.setHiddenWalletPinFragment)
                    }
                },
                onNavigateBack = navController::navigateUpSafely
            )
        }
        composablePage(AdvancedSecurityRoutes.SECURE_RESET_TERMS_PAGE) {
            val context = LocalContext.current
            val termTitles = context.resources.getStringArray(R.array.SecureReset_Terms_Checkboxes)

            val termsViewModel: SecureResetTermsViewModel = koinViewModel {
                parametersOf(termTitles)
            }
            val uiState = termsViewModel.uiState

            SecureResetTermsScreen(
                uiState = uiState,
                onCheckboxToggle = termsViewModel::toggleCheckbox,
                onAgreeClick = {
                    fragmentNavController.ensurePinSet(R.string.PinSet_Title) {
                        fragmentNavController.slideFromRight(R.id.setSecureResetPinFragment)
                        viewModel.onSecureResetEnabled()
                    }
                },
                onNavigateBack = navController::navigateUpSafely
            )
        }
        composablePage(AdvancedSecurityRoutes.CALCULATOR_PIN_PAGE) {
            val pinViewModel: CalculatorPinSettingsViewModel = koinViewModel()
            val activity = LocalActivity.current
            CalculatorPinSettingsScreen(
                uiState = pinViewModel.uiState,
                onToggleCalculator = { enabled, disablePushNotifications ->
                    val calculatorModeService = getKoinInstance<CalculatorModeService>()
                    if (enabled) {
                        val pinExistedBefore = getKoinInstance<IPinComponent>().isPinSet
                        fragmentNavController.premiumAction {
                            fragmentNavController.ensurePinSet(R.string.PinSet_Title) {
                                calculatorModeService.enable(
                                    pinExistedBefore = pinExistedBefore,
                                    disablePushNotifications = disablePushNotifications,
                                )
                                activity?.fullRestart()
                            }
                        }
                    } else {
                        fragmentNavController.authorizedAction {
                            calculatorModeService.disable()
                            activity?.fullRestart()
                        }
                    }
                },
                onAutoLockClick = {
                    navController.navigate(AdvancedSecurityRoutes.CALCULATOR_AUTO_LOCK_PAGE)
                },
                onClose = navController::navigateUp,
            )
        }
        composablePage(AdvancedSecurityRoutes.CALCULATOR_AUTO_LOCK_PAGE) {
            val autoLockViewModel: CalculatorAutoLockViewModel = koinViewModel()
            CalculatorAutoLockScreen(
                uiState = autoLockViewModel.uiState,
                onSelect = { option ->
                    autoLockViewModel.onSelect(option)
                    navController.navigateUp()
                },
                onClose = navController::navigateUp,
            )
        }
    }
}
