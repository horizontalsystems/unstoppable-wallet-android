package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statAccountType
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.MainScreen
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderColored
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class CreateAccountPasskeyFragment(val input: ManageAccountsModule.Input?) : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val popUpToInclusiveId = input?.popOffOnSuccess ?: CreateAccountFragment::class
        val inclusive = input?.popOffInclusive ?: true
        CreateAccountPasskeyScreen(navController, popUpToInclusiveId, inclusive)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountPasskeyScreen(
    navController: NavBackStack<HSScreen>,
    popUpToInclusiveId: KClass<out HSScreen>,
    inclusive: Boolean
) {
    val viewModel =
        viewModel<CreateAccountPasskeyViewModel>(factory = CreateAccountPasskeyViewModel.Factory())
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Local state — disables the Create button immediately on click to prevent double-submission
    // while the system passkey UI is open (per CLAUDE.md double-click prevention pattern).
    var createButtonEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.success) {
        if (uiState.success != null) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Created,
            )
            delay(300)
            navController.removeLastUntil(popUpToInclusiveId, inclusive)
            stat(
                page = StatPage.NewWalletPasskey,
                event = StatEvent.CreateWallet(uiState.success.statAccountType)
            )
        }
    }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        HudHelper.showErrorMessage(view, error)
        viewModel.onErrorDisplayed()
    }

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_CreateNewWallet),
        onBack = navController::removeLastOrNull,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                SectionHeaderColored(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = ComposeAppTheme.colors.grey,
                    title = stringResource(id = R.string.ManageAccount_WalletName)
                )
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = uiState.accountName,
                    pasteEnabled = false,
                    hint = uiState.defaultAccountName,
                    onValueChange = viewModel::onChangeAccountName,
                    trailingContent = {
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            HSIconButton(
                                variant = ButtonVariant.Secondary,
                                size = ButtonSize.Small,
                                icon = painterResource(R.drawable.ic_swap_circle_24),
                                onClick = viewModel::generateRandomAccountName
                            )
                        }
                    }
                )

                VSpacer(32.dp)
            }
            ButtonsGroupWithShade {
                HSButton(
                    title = stringResource(R.string.Button_Create),
                    variant = ButtonVariant.Primary,
                    enabled = createButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    onClick = {
                        val accountName = uiState.accountName.ifBlank { uiState.defaultAccountName }
                        createButtonEnabled = false
                        scope.launch {
                            try {
                                val entropy = App.passkeyManager.register(
                                    context = context,
                                    accountName = accountName,
                                )
                                viewModel.createAccount(entropy)
                            } catch (e: CreateCredentialCancellationException) {
                            } catch (e: CreateCredentialNoCreateOptionException) {
                                navController.slideFromBottom(R.id.createPasskeyNotSupported)
                            } catch (e: Exception) {
                                viewModel.onError(e)
                            } finally {
                                createButtonEnabled = true
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
@Preview
fun Preview_CreateAccountPasskeyScreen() {
    ComposeAppTheme {
        CreateAccountPasskeyScreen(
            NavBackStack(),
            MainScreen::class,
            false
        )
    }
}
