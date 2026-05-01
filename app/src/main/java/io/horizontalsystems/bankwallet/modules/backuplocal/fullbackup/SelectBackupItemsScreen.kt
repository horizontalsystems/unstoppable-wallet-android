package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.Section
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderColored

@Composable
fun SelectBackupItemsScreen(
    onNextClick: (accountIds: List<String>, sections: Set<BackupSection>) -> Unit,
    onBackClick: () -> Unit
) {
    val viewModel =
        viewModel<SelectBackupItemsViewModel>(factory = SelectBackupItemsViewModel.Factory())
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.BackupManager_CreateBackup),
        onBack = onBackClick,
        bottomBar = {
            ButtonsGroupWithShade {
                HSButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        onNextClick(viewModel.selectedWallets, viewModel.selectedSections)
                    }
                )
            }
        }
    ) {
        LazyColumn {
            item {
                TextBlock(text = stringResource(R.string.BackupManager_SelectItemsDescription))
            }

            when (uiState.viewState) {
                ViewState.Success -> {
                    if (uiState.wallets.isNotEmpty()) {
                        item {
                            SectionHeaderColored(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = ComposeAppTheme.colors.grey,
                                title = stringResource(R.string.BackupManager_Wallets)
                            )
                            Section {
                                uiState.wallets.forEachIndexed { index, wallet ->
                                    if (index > 0) Divider(color = ComposeAppTheme.colors.blade)
                                    CellPrimary(
                                        middle = {
                                            CellMiddleInfo(
                                                title = wallet.name.hs,
                                                subtitle = if (wallet.backupRequired) {
                                                    stringResource(R.string.BackupManager_BackupRequired).hs(
                                                        color = ComposeAppTheme.colors.lucian
                                                    )
                                                } else {
                                                    wallet.type.hs
                                                }
                                            )
                                        },
                                        right = {
                                            CellRightSelectors(
                                                icon = painterResource(id = if (wallet.selected) R.drawable.selector_checked_20 else R.drawable.selector_unchecked_20),
                                                iconTint = if (wallet.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey
                                            )
                                        },
                                        onClick = { viewModel.toggle(wallet) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeaderColored(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = ComposeAppTheme.colors.grey,
                            title = stringResource(R.string.BackupManager_Other)
                        )
                        Section {
                            uiState.otherBackupItems.forEachIndexed { index, item ->
                                if (index > 0) Divider(color = ComposeAppTheme.colors.blade)
                                CellPrimary(
                                    middle = {
                                        CellMiddleInfo(
                                            title = item.title.hs,
                                            subtitle = (item.value ?: item.subtitle)?.hs
                                        )
                                    },
                                    right = {
                                        CellRightSelectors(
                                            icon = painterResource(id = if (item.selected) R.drawable.selector_checked_20 else R.drawable.selector_unchecked_20),
                                            iconTint = if (item.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey
                                        )
                                    },
                                    onClick = if (item.section != null) {
                                        { viewModel.toggleOtherItem(item) }
                                    } else null
                                )
                            }
                        }
                        VSpacer(height = 32.dp)
                    }
                }

                is ViewState.Error,
                ViewState.Loading -> Unit
            }
        }
    }
}
