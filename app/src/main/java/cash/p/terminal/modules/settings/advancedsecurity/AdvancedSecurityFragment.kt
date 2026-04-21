package cash.p.terminal.modules.settings.advancedsecurity

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.authorizedDeleteContactsPasscodeAction
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.ensurePinSet
import cash.p.terminal.core.premiumAction
import cash.p.terminal.core.slideToDeleteContactsTerms
import cash.p.terminal.modules.settings.advancedsecurity.securereset.SecureResetTermsScreen
import cash.p.terminal.modules.settings.advancedsecurity.securereset.SecureResetTermsViewModel
import cash.p.terminal.modules.settings.advancedsecurity.terms.HiddenWalletTermsScreen
import cash.p.terminal.modules.settings.advancedsecurity.terms.HiddenWalletTermsViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.BaseComposeFragment
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
    val viewModel: AdvancedSecurityViewModel = koinViewModel { parametersOf(App.pinComponent) }

    NavHost(
        navController = navController,
        startDestination = AdvancedSecurityRoutes.ADVANCED_SECURITY_PAGE
    ) {
        composable(AdvancedSecurityRoutes.ADVANCED_SECURITY_PAGE) {
            AdvancedSecurityScreen(
                uiState = viewModel.uiState,
                onCreateHiddenWalletClick = {
                    fragmentNavController.premiumAction {
                        navController.navigate(AdvancedSecurityRoutes.HIDDEN_WALLET_TERM_SPAGE)
                    }
                },
                onSecureResetToggle = { enabled ->
                    if (enabled) {
                        fragmentNavController.premiumAction {
                            navController.navigate(AdvancedSecurityRoutes.SECURE_RESET_TERMS_PAGE)
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
                onClose = fragmentNavController::navigateUp
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
                onNavigateBack = navController::navigateUp
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
                onNavigateBack = navController::navigateUp
            )
        }
    }
}
