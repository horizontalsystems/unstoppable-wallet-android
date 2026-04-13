package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class CreateAccountPasskeyFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        CreateAccountPasskeyScreen(navController)
    }
}

@Composable
fun CreateAccountPasskeyScreen(navController: NavController) {
    val viewModel = viewModel<CreateAccountPasskeyViewModel>(factory = CreateAccountPasskeyViewModel.Factory())
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_CreateNewWallet),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Create),
                onClick = viewModel::createAccount,
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
        CreateAccountPasskeyScreen(NavController(LocalContext.current))
    }
}
