package cash.p.terminal.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.createaccount.passphraseterms.PassphraseTermsScreen
import cash.p.terminal.modules.createaccount.passphraseterms.PassphraseTermsViewModel
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restoreaccount.duplicatewallet.DuplicateWalletScreen
import cash.p.terminal.modules.restoreaccount.duplicatewallet.DuplicateWalletViewModel
import cash.p.terminal.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuModule
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestorePhrase
import cash.p.terminal.modules.restoreaccount.restoremnemonicnonstandard.RestorePhraseNonStandard
import cash.p.terminal.navigation.navigateUpSafely
import cash.p.terminal.navigation.popBackStackSafely
import cash.p.terminal.strings.helpers.Translator.getString
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.wallet.IAccountManager
import io.horizontalsystems.hdwalletkit.Language
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

class RestoreAccountFragment : BaseComposeFragment(screenshotEnabled = false) {

    companion object {
        const val ROUTE_DUPLICATE = "duplicate_wallet"
        const val ROUTE_RESTORE_PHRASE = "restore_phrase"
        const val ROUTE_RESTORE_PHRASE_ADVANCED = "restore_phrase_advanced"
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        val popUpToInclusiveId = input?.popOffOnSuccess ?: R.id.restoreAccountFragment
        val inclusive = input?.popOffInclusive ?: false
        val defaultRoute = input?.defaultRoute ?: ROUTE_RESTORE_PHRASE

        RestoreAccountNavHost(
            fragmentNavController = navController,
            popUpToInclusiveId = popUpToInclusiveId,
            inclusive = inclusive,
            defaultRoute = defaultRoute,
            accountId = input?.accountId.orEmpty(),
            prefillWords = input?.prefillWords,
            prefillPassphrase = input?.prefillPassphrase,
            prefillMoneroHeight = input?.prefillMoneroHeight,
            prefillMnemonicLanguage = input?.prefillMnemonicLanguage
        )
    }

}

@Composable
private fun RestoreAccountNavHost(
    fragmentNavController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean,
    defaultRoute: String,
    accountId: String,
    prefillWords: List<String>? = null,
    prefillPassphrase: String? = null,
    prefillMoneroHeight: Long? = null,
    prefillMnemonicLanguage: Language? = null
) {
    val navController = rememberNavController()
    val restoreMenuViewModel: RestoreMenuViewModel =
        viewModel(factory = RestoreMenuModule.Factory())
    val mainViewModel: RestoreViewModel = viewModel()

    // Initialize prefill data in shared ViewModel
    LaunchedEffect(prefillWords, prefillPassphrase, prefillMoneroHeight, prefillMnemonicLanguage) {
        mainViewModel.setPrefillData(
            prefillWords,
            prefillPassphrase,
            prefillMoneroHeight,
            prefillMnemonicLanguage
        )
    }

    // Navigate to advanced screen if passphrase is present from QR code
    val actualStartDestination = if (!prefillPassphrase.isNullOrEmpty()) {
        RestoreAccountFragment.ROUTE_RESTORE_PHRASE_ADVANCED
    } else {
        defaultRoute
    }

    NavHost(
        navController = navController,
        startDestination = actualStartDestination,
    ) {
        composable(RestoreAccountFragment.ROUTE_RESTORE_PHRASE) {
            RestorePhrase(
                advanced = false,
                restoreMenuViewModel = restoreMenuViewModel,
                mainViewModel = mainViewModel,
                openRestoreAdvanced = { navController.navigate(RestoreAccountFragment.ROUTE_RESTORE_PHRASE_ADVANCED) },
                openSelectCoins = { navController.navigate("restore_select_coins") },
                openNonStandardRestore = { navController.navigate("restore_phrase_nonstandard") },
                onBackClick = { fragmentNavController.popBackStackSafely() },
                onFinish = { fragmentNavController.popBackStack(popUpToInclusiveId, inclusive) },
                prefillWords = prefillWords,
                prefillPassphrase = prefillPassphrase,
                prefillMoneroHeight = prefillMoneroHeight,
                prefillMnemonicLanguage = prefillMnemonicLanguage
            )
        }
        composablePage(RestoreAccountFragment.ROUTE_RESTORE_PHRASE_ADVANCED) {
            AdvancedRestoreScreen(
                restoreMenuViewModel = restoreMenuViewModel,
                mainViewModel = mainViewModel,
                openSelectCoinsScreen = { navController.navigate("restore_select_coins") },
                openNonStandardRestore = {
                    navController.navigate("restore_phrase_nonstandard")
                },
                onBackClick = {
                    if (!navController.popBackStackSafely()) {
                        fragmentNavController.popBackStackSafely()
                    }
                },
                onFinish = { fragmentNavController.popBackStack(popUpToInclusiveId, inclusive) },
                prefillWords = mainViewModel.prefillWords ?: prefillWords,
                prefillPassphrase = mainViewModel.prefillPassphrase ?: prefillPassphrase,
                prefillMoneroHeight = mainViewModel.prefillMoneroHeight ?: prefillMoneroHeight,
                prefillMnemonicLanguage = mainViewModel.prefillMnemonicLanguage
                    ?: prefillMnemonicLanguage
            )
        }
        composablePage(RestoreAccountFragment.ROUTE_DUPLICATE) { backStackEntry ->
            val accountManager: IAccountManager by inject(IAccountManager::class.java)
            val accountToCopy = remember { accountManager.account(accountId) }
            if (accountToCopy == null) {
                val view = LocalView.current
                LaunchedEffect(Unit) {
                    HudHelper.showErrorMessage(view, getString(R.string.error_no_active_account))
                    fragmentNavController.popBackStack()
                }
                return@composablePage
            }

            val viewModel: DuplicateWalletViewModel = koinViewModel(
                parameters = { parametersOf(accountToCopy) }
            )
            val passphraseTermsAgreed by backStackEntry.savedStateHandle
                .getStateFlow("passphrase_terms_agreed", false)
                .collectAsStateWithLifecycle()

            LaunchedEffect(passphraseTermsAgreed) {
                if (passphraseTermsAgreed) {
                    viewModel.onPassphraseTermsAgreed()
                    backStackEntry.savedStateHandle["passphrase_terms_agreed"] = false
                }
            }
            DuplicateWalletScreen(
                uiState = viewModel.uiState,
                passphraseTermsAccepted = viewModel.passphraseTermsAgreed,
                onEnterName = viewModel::onEnterName,
                onTogglePassphrase = viewModel::onTogglePassphrase,
                onChangePassphrase = viewModel::onChangePassphrase,
                onChangePassphraseConfirmation = viewModel::onChangePassphraseConfirmation,
                onCreate = viewModel::createAccount,
                onBackClick = { fragmentNavController.popBackStackSafely() },
                onOpenTerms = {
                    navController.navigate("passphrase_terms")
                },
                onFinish = { fragmentNavController.popBackStack(popUpToInclusiveId, inclusive) }
            )
        }
        composablePage("passphrase_terms") {
            val context = LocalContext.current
            val termTitles = context.resources.getStringArray(R.array.passphrase_terms_checkboxes)
            val viewModel = koinViewModel<PassphraseTermsViewModel> { parametersOf(termTitles) }

            PassphraseTermsScreen(
                uiState = viewModel.uiState,
                onCheckboxToggle = viewModel::toggleCheckbox,
                onAgreeClick = {
                    viewModel.agree()
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("passphrase_terms_agreed", true)
                    navController.navigateUpSafely()
                },
                onBackClick = navController::navigateUpSafely
            )
        }
        composablePage("restore_select_coins") {
            ManageWalletsScreen(
                mainViewModel = mainViewModel,
                openConfigure = { token, initialConfig ->
                    navController.openRestoreTokenConfigure(token, initialConfig, mainViewModel)
                },
                onBackClick = { navController.popBackStackSafely() }
            ) { fragmentNavController.popBackStack(popUpToInclusiveId, inclusive) }
        }
        composablePage("restore_phrase_nonstandard") {
            RestorePhraseNonStandard(
                mainViewModel = mainViewModel,
                openSelectCoinsScreen = { navController.navigate("restore_select_coins") },
                onBackClick = { navController.popBackStackSafely() }
            )
        }
        addRestoreTokenConfigureRoutes(navController, mainViewModel)
    }
}
