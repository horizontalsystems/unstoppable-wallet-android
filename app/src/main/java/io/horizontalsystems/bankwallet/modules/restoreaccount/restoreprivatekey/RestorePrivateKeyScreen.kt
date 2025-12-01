package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreByMenu
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputMultiline
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

@Composable
fun RestorePrivateKey(
    restoreMenuViewModel: RestoreMenuViewModel,
    mainViewModel: RestoreViewModel,
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
                    viewModel.resolveAccountType()?.let { accountType ->
                        mainViewModel.setAccountData(
                            accountType,
                            viewModel.accountName,
                            true,
                            false,
                            StatPage.ImportWalletFromKeyAdvanced
                        )
                        openSelectCoinsScreen.invoke()

                        stat(
                            page = StatPage.ImportWalletFromKeyAdvanced,
                            event = StatEvent.Open(StatPage.RestoreSelect)
                        )
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
