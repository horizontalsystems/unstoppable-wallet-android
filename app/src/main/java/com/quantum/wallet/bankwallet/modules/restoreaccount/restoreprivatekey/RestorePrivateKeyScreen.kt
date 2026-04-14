package com.quantum.wallet.bankwallet.modules.restoreaccount.restoreprivatekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.restoreaccount.RestoreViewModel
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoremenu.RestoreByMenu
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.FormsInput
import com.quantum.wallet.bankwallet.ui.compose.components.FormsInputMultiline
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderText
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

@Composable
fun RestorePrivateKey(
    restoreMenuViewModel: RestoreMenuViewModel,
    mainViewModel: RestoreViewModel,
    openSelectNetworkScreen: () -> Unit,
    openSelectCoinsScreen: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel =
        viewModel<RestorePrivateKeyViewModel>(factory = RestorePrivateKeyModule.Factory())

    HSScaffold(
        title = stringResource(R.string.Restore_Advanced_Title),
        onBack = onBackClick,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Next),
                onClick = {
                    val accountTypes = viewModel.resolveAccountTypes()
                    if (accountTypes.isNotEmpty()) {
                        mainViewModel.setAccountData(
                            accountTypes.singleOrNull(),
                            viewModel.accountName,
                            true,
                            false,
                            StatPage.ImportWalletFromKeyAdvanced
                        )
                        if (accountTypes.size == 1) {
                            openSelectCoinsScreen.invoke()

                            stat(
                                page = StatPage.ImportWalletFromKeyAdvanced,
                                event = StatEvent.Open(StatPage.RestoreSelect)
                            )
                        } else {
                            mainViewModel.accountTypes = accountTypes
                            openSelectNetworkScreen()
                        }
                    }
                },
                tint = ComposeAppTheme.colors.jacob
            )
        )
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)

            HeaderText(stringResource(id = R.string.ManageAccount_Name))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = viewModel.accountName,
                pasteEnabled = false,
                hint = viewModel.defaultName,
                onValueChange = viewModel::onEnterName
            )
            VSpacer(32.dp)

            RestoreByMenu(restoreMenuViewModel)

            VSpacer(32.dp)

            FormsInputMultiline(
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(id = R.string.Restore_PrivateKeyHint),
                state = viewModel.inputState,
                qrScannerEnabled = true,
                onValueChange = {
                    viewModel.onEnterPrivateKey(it)
                },
                onClear = {
                    stat(
                        page = StatPage.ImportWalletFromKeyAdvanced,
                        event = StatEvent.Clear(StatEntity.Key)
                    )
                },
                onPaste = {
                    stat(
                        page = StatPage.ImportWalletFromKeyAdvanced,
                        event = StatEvent.Paste(StatEntity.Key)
                    )
                },
                onScanQR = {
                    stat(
                        page = StatPage.ImportWalletFromKeyAdvanced,
                        event = StatEvent.ScanQr(StatEntity.Key)
                    )
                })

            VSpacer(32.dp)
        }
    }
}
