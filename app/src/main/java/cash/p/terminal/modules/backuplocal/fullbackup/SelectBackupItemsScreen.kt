package cash.p.terminal.modules.backuplocal.fullbackup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HeaderText
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.HsCheckbox
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead1_grey
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_lucian

@Composable
fun SelectBackupItemsScreen(
    onNextClick: (accountIds: List<String>) -> Unit,
    onBackClick: () -> Unit
) {
    val viewModel = viewModel<SelectBackupItemsViewModel>(factory = SelectBackupItemsViewModel.Factory())
    val uiState = viewModel.uiState

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.BackupManager_BаckupFile),
                navigationIcon = {
                    HsBackButton(onClick = onBackClick)
                },
            )
        },
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
        LazyColumn(modifier = Modifier.padding(it)) {

            when (uiState.viewState) {
                ViewState.Success -> {
                    if (uiState.wallets.isNotEmpty()) {
                        item {
                            HeaderText(text = stringResource(id = R.string.BackupManager_Wallets))
                            CellUniversalLawrenceSection(items = uiState.wallets, showFrame = true) { wallet ->
                                RowUniversal(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick = { viewModel.toggle(wallet) }
                                ) {

                                    Column(modifier = Modifier.weight(1f)) {
                                        body_leah(text = wallet.name)
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
