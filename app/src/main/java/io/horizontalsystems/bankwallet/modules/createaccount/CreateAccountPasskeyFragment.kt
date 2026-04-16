package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statAccountType
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CreateAccountPasskeyFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        val popUpToInclusiveId = input?.popOffOnSuccess ?: R.id.createAccountFragment
        val inclusive = input?.popOffInclusive ?: true
        CreateAccountPasskeyScreen(navController, popUpToInclusiveId, inclusive)
    }
}

@Composable
fun CreateAccountPasskeyScreen(
    navController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val viewModel = viewModel<CreateAccountPasskeyViewModel>(factory = CreateAccountPasskeyViewModel.Factory())
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
            navController.popBackStack(popUpToInclusiveId, inclusive)
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
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Create),
                enabled = createButtonEnabled,
                onClick = {
                    val accountName = uiState.accountName
                    createButtonEnabled = false
                    scope.launch {
                        try {
                            val entropy = App.passkeyManager.register(
                                context = context,
                                accountName = accountName,
                            )
                            viewModel.createAccount(entropy)
                        } catch (e: Exception) {
                            viewModel.onError(e)
                        } finally {
                            createButtonEnabled = true
                        }
                    }
                },
                tint = ComposeAppTheme.colors.jacob
            )
        ),
    ) {
        Column {
            VSpacer(12.dp)

            HeaderText(stringResource(id = R.string.ManageAccount_Name))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = uiState.defaultAccountName,
                pasteEnabled = false,
                hint = uiState.defaultAccountName,
                onValueChange = viewModel::onChangeAccountName
            )

            VSpacer(32.dp)
        }
    }
}

@Composable
@Preview
fun Preview_CreateAccountPasskeyScreen() {
    ComposeAppTheme {
        CreateAccountPasskeyScreen(
            NavController(LocalContext.current),
            0,
            false
        )
    }
}
