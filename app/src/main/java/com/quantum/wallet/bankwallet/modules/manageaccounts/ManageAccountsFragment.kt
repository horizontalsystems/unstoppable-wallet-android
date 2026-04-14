package com.quantum.wallet.bankwallet.modules.manageaccounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.navigateWithTermsAccepted
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.manageaccount.ManageAccountFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.dialogs.BackupRequiredAlert
import com.quantum.wallet.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import com.quantum.wallet.bankwallet.modules.manageaccounts.ManageAccountsModule.ActionViewItem
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonSecondaryCircle
import com.quantum.wallet.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.HsRadioButton
import com.quantum.wallet.bankwallet.ui.compose.components.RowUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.body_jacob
import com.quantum.wallet.bankwallet.ui.compose.components.headline2_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_lucian
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

class ManageAccountsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<ManageAccountsModule.Mode>(navController) { input ->
            ManageAccountsScreen(navController, input)
        }
    }
}

@Composable
fun ManageAccountsScreen(navController: NavController, mode: ManageAccountsModule.Mode) {
    BackupRequiredAlert(navController)

    val viewModel = viewModel<ManageAccountsViewModel>(factory = ManageAccountsModule.Factory(mode))

    val viewItems = viewModel.viewItems
    val finish = viewModel.finish

    if (finish) {
        navController.popBackStack()
    }

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_Title),
        onBack = navController::popBackStack,
    ) {
        LazyColumn(
            modifier = Modifier.navigationBarsPadding()
        ) {
            item {
                VSpacer(12.dp)

                viewItems?.let { (regularAccounts, watchAccounts) ->
                    if (regularAccounts.isNotEmpty()) {
                        AccountsSection(regularAccounts, viewModel, navController)
                        VSpacer(32.dp)
                    }

                    if (watchAccounts.isNotEmpty()) {
                        AccountsSection(watchAccounts, viewModel, navController)
                        VSpacer(32.dp)
                    }
                }

                val args = when (mode) {
                    ManageAccountsModule.Mode.Manage -> ManageAccountsModule.Input(
                        R.id.manageAccountsFragment,
                        false
                    )

                    ManageAccountsModule.Mode.Switcher -> ManageAccountsModule.Input(
                        R.id.manageAccountsFragment,
                        true
                    )
                }

                val actions = listOf(
                    ActionViewItem(
                        R.drawable.ic_plus,
                        R.string.ManageAccounts_CreateNewWallet
                    ) {
                        navController.navigateWithTermsAccepted {
                            navController.slideFromRight(R.id.createAccountFragment, args)

                            stat(
                                page = StatPage.ManageWallets,
                                event = StatEvent.Open(StatPage.NewWallet)
                            )
                        }
                    },
                    ActionViewItem(
                        R.drawable.ic_download_20,
                        R.string.ManageAccounts_ImportWallet
                    ) {
                        navController.slideFromRight(R.id.importWalletFragment, args)

                        stat(
                            page = StatPage.ManageWallets,
                            event = StatEvent.Open(StatPage.ImportWallet)
                        )
                    },
                    ActionViewItem(
                        R.drawable.icon_binocule_20,
                        R.string.ManageAccounts_WatchAddress
                    ) {
                        navController.slideFromRight(R.id.watchAddressFragment, args)

                        stat(
                            page = StatPage.ManageWallets,
                            event = StatEvent.Open(StatPage.WatchWallet)
                        )
                    }
                )
                CellUniversalLawrenceSection(actions) {
                    RowUniversal(
                        onClick = it.callback
                    ) {
                        Icon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            painter = painterResource(id = it.icon),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.jacob
                        )
                        body_jacob(text = stringResource(id = it.title))
                    }
                }

                VSpacer(32.dp)
            }
        }
    }
}

@Composable
private fun AccountsSection(
    accounts: List<AccountViewItem>,
    viewModel: ManageAccountsViewModel,
    navController: NavController
) {
    CellUniversalLawrenceSection(items = accounts) { accountViewItem ->
        RowUniversal(
            onClick = {
                viewModel.onSelect(accountViewItem)

                stat(page = StatPage.ManageWallets, event = StatEvent.Select(StatEntity.Wallet))
            }
        ) {
            HsRadioButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                selected = accountViewItem.selected,
                onClick = {
                    viewModel.onSelect(accountViewItem)
                    stat(page = StatPage.ManageWallets, event = StatEvent.Select(StatEntity.Wallet))
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                headline2_leah(text = accountViewItem.title)
                if (accountViewItem.backupRequired) {
                    subhead2_lucian(text = stringResource(id = R.string.ManageAccount_BackupRequired_Title))
                } else if (accountViewItem.migrationRequired) {
                    subhead2_lucian(text = stringResource(id = R.string.ManageAccount_MigrationRequired_Title))
                } else {
                    subhead2_grey(
                        text = accountViewItem.subtitle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            if (accountViewItem.isWatchAccount) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_binocule_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }

            val icon: Int
            val iconTint: Color
            if (accountViewItem.showAlertIcon) {
                icon = R.drawable.icon_warning_2_20
                iconTint = ComposeAppTheme.colors.lucian
            } else {
                icon = R.drawable.ic_more2_20
                iconTint = ComposeAppTheme.colors.leah
            }

            ButtonSecondaryCircle(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = icon,
                tint = iconTint
            ) {
                navController.slideFromRight(
                    R.id.manageAccountFragment,
                    ManageAccountFragment.Input(accountViewItem.accountId)
                )

                stat(page = StatPage.ManageWallets, event = StatEvent.Open(StatPage.ManageWallet))
            }
        }
    }
}
