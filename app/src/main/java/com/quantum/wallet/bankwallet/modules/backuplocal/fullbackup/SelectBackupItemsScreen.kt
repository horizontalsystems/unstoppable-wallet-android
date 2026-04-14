package com.quantum.wallet.bankwallet.modules.backuplocal.fullbackup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.evmfee.ButtonsGroupWithShade
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderText
import com.quantum.wallet.bankwallet.ui.compose.components.HsCheckbox
import com.quantum.wallet.bankwallet.ui.compose.components.RowUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.body_leah
import com.quantum.wallet.bankwallet.ui.compose.components.headline2_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_grey
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_lucian
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

@Composable
fun SelectBackupItemsScreen(
    onNextClick: (accountIds: List<String>) -> Unit,
    onBackClick: () -> Unit
) {
    val viewModel =
        viewModel<SelectBackupItemsViewModel>(factory = SelectBackupItemsViewModel.Factory())
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.BackupManager_BаckupFile),
        onBack = onBackClick,
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        onNextClick(viewModel.selectedWallets)
                    }
                )
            }
        }
    ) {
        LazyColumn {
            when (uiState.viewState) {
                ViewState.Success -> {
                    if (uiState.wallets.isNotEmpty()) {
                        item {
                            HeaderText(text = stringResource(id = R.string.BackupManager_Wallets))
                            CellUniversalLawrenceSection(
                                items = uiState.wallets,
                                showFrame = true
                            ) { wallet ->
                                RowUniversal(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick = { viewModel.toggle(wallet) }
                                ) {

                                    Column(modifier = Modifier.weight(1f)) {
                                        headline2_leah(text = wallet.name)
                                        if (wallet.backupRequired) {
                                            subhead2_lucian(text = stringResource(id = R.string.BackupManager_BackupRequired))
                                        } else {
                                            subhead2_grey(
                                                text = wallet.type,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    HsCheckbox(
                                        checked = wallet.selected,
                                        onCheckedChange = {
                                            viewModel.toggle(wallet)
                                        },
                                    )
                                }
                            }
                            VSpacer(height = 24.dp)
                        }
                    }

                    item {
                        OtherBackupItems(uiState.otherBackupItems)
                        VSpacer(height = 32.dp)
                    }
                }

                is ViewState.Error,
                ViewState.Loading -> Unit
            }
        }
    }
}

@Composable
fun OtherBackupItems(otherBackupItems: List<SelectBackupItemsViewModel.OtherBackupViewItem>) {
    HeaderText(text = stringResource(id = R.string.BackupManager_Other))
    CellUniversalLawrenceSection(items = otherBackupItems, showFrame = true) { item ->
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    body_leah(text = item.title, modifier = Modifier.weight(1f))
                    item.value?.let {
                        subhead1_grey(
                            text = item.value,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
                item.subtitle?.let {
                    subhead2_grey(
                        text = item.subtitle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
