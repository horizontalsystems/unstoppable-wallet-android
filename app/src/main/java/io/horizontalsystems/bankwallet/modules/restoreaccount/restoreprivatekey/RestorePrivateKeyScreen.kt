package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreByMenu
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputMultiline
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

@Composable
fun RestorePrivateKey(
    restoreMenuViewModel: RestoreMenuViewModel,
    mainViewModel: RestoreViewModel,
    openSelectCoinsScreen: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel = viewModel<RestorePrivateKeyViewModel>(factory = RestorePrivateKeyModule.Factory())

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.Restore_Advanced_Title),
                navigationIcon = {
                    HsBackButton(onClick = onBackClick)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Next),
                        onClick = {
                            viewModel.resolveAccountType()?.let { accountType ->
                                mainViewModel.setAccountData(accountType, viewModel.accountName, true, false)
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
            ) {
                viewModel.onEnterPrivateKey(it)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
