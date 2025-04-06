package cash.p.terminal.modules.restoreaccount.restoreprivatekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.stats.StatEntity
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.modules.restoreaccount.RestoreViewModel
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreByMenu
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui.compose.components.FormsInputMultiline
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun RestorePrivateKey(
    restoreMenuViewModel: RestoreMenuViewModel,
    mainViewModel: RestoreViewModel,
    openSelectCoinsScreen: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel = viewModel<RestorePrivateKeyViewModel>(factory = RestorePrivateKeyModule.Factory())

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Restore_Advanced_Title),
                navigationIcon = {
                    HsBackButton(onClick = onBackClick)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Next),
                        onClick = {
                            viewModel.resolveAccountType()?.let { accountType ->
                                mainViewModel.setAccountData(accountType, viewModel.accountName, true, false, StatPage.ImportWalletFromKeyAdvanced)
                                openSelectCoinsScreen.invoke()
                            }
                        }
                    )
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            HeaderText(stringResource(id = R.string.ManageAccount_Name))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = viewModel.accountName,
                pasteEnabled = false,
                hint = viewModel.defaultName,
                onValueChange = viewModel::onEnterName
            )
            Spacer(Modifier.height(32.dp))

            RestoreByMenu(restoreMenuViewModel)

            Spacer(Modifier.height(32.dp))

            FormsInputMultiline(
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(id = R.string.Restore_PrivateKeyHint),
                state = viewModel.inputState,
                qrScannerEnabled = true,
                onValueChange = {
                    viewModel.onEnterPrivateKey(it)
                },
                onClear = {
                },
                onPaste = {
                },
                onScanQR = {
                })

            Spacer(Modifier.height(32.dp))
        }
    }
}
