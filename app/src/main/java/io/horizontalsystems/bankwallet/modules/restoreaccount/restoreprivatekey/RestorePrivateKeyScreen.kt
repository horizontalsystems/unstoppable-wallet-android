package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputMultiline
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderColored

@Composable
fun RestorePrivateKey(
    mainViewModel: RestoreViewModel,
    openSelectNetworkScreen: () -> Unit,
    openSelectCoinsScreen: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel =
        viewModel<RestorePrivateKeyViewModel>(factory = RestorePrivateKeyModule.Factory())

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_ImportWallet),
        onBack = onBackClick,
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
                    initial = viewModel.accountName,
                    pasteEnabled = false,
                    hint = viewModel.defaultName,
                    onValueChange = viewModel::onEnterName,
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
                VSpacer(24.dp)

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
            ButtonsGroupWithShade {
                HSButton(
                    title = stringResource(R.string.Button_Create),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
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
                )
            }
        }
    }
}
