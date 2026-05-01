package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.NavigationType
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.createaccount.CreateAccountFragment
import io.horizontalsystems.bankwallet.modules.importwallet.ImportWalletFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredAlert
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemDropdown
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsIconButton
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderColored

class ManageAccountsFragment(val input: ManageAccountsModule.Mode) : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        ManageAccountsScreen(navController, input)
    }
}

@Composable
fun ManageAccountsScreen(navController: NavBackStack<HSScreen>, mode: ManageAccountsModule.Mode) {
    BackupRequiredAlert(navController)

    val viewModel = viewModel<ManageAccountsViewModel>(factory = ManageAccountsModule.Factory(mode))
    var searchQuery by remember { mutableStateOf(viewModel.searchQuery) }
    var isSearchActive by remember { mutableStateOf(false) }

    val viewItems = viewModel.viewItems
    val finish = viewModel.finish

    LaunchedEffect(finish) {
        if (finish) {
            navController.removeLastOrNull()
        }
    }

    val args = when (mode) {
        ManageAccountsModule.Mode.Manage -> ManageAccountsModule.Input(
            ManageAccountsFragment::class,
            false
        )

        ManageAccountsModule.Mode.Switcher -> ManageAccountsModule.Input(
            ManageAccountsFragment::class,
            true
        )
    }

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_Title),
        onBack = navController::removeLastOrNull,
        menuItems = listOf(
            MenuItemDropdown(
                title = TranslatableString.ResString(R.string.Button_Add),
                icon = R.drawable.wallet_add_sharp_24,
                items = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.ManageAccounts_CreateNewWallet),
                        onClick = {
                            navController.navigateWithTermsAccepted(
                                screen = CreateAccountFragment(args),
                                navigationType = NavigationType.SlideFromRight,
                                statPageFrom = StatPage.ManageWallets,
                                statPageTo = StatPage.NewWallet
                            )
                        }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.ManageAccounts_ExistingWallet),
                        onClick = {
                            navController.navigateWithTermsAccepted(
                                screen = ImportWalletFragment(args),
                                navigationType = NavigationType.SlideFromRight,
                                statPageFrom = StatPage.ManageWallets,
                                statPageTo = StatPage.ImportWallet
                            )
                        }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.ManageAccounts_ViewOnlyWallet),
                        onClick = {
                            navController.slideFromRight(WatchAddressFragment(args))
                            stat(page = StatPage.ManageWallets, event = StatEvent.Open(StatPage.WatchWallet))
                        }
                    ),
                )
            )
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewItems == null || (viewItems.first.isEmpty() && viewItems.second.isEmpty())) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeAppTheme.colors.lawrence),
                ) {
                    if (searchQuery.isNotEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.EmptyResults),
                            icon = R.drawable.ic_not_found
                        )
                    } else {
                        ListEmptyView(
                            text = stringResource(R.string.ManageAccounts_NoActiveWallets),
                            icon = R.drawable.wallet_remove_24
                        )
                    }
                }
            } else {
                val (regularAccounts, watchAccounts) = viewItems
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .background(ComposeAppTheme.colors.lawrence)
                        .navigationBarsPadding(),
                ) {
                    if (regularAccounts.isNotEmpty()) {
                        item {
                            BoxBordered(bottom = true) {
                                SectionHeaderColored(title = stringResource(R.string.ManageAccount_Wallets))
                            }
                        }
                        regularAccounts.forEach { account ->
                            item {
                                AccountCellWrapper(account, viewModel, navController)
                            }
                        }
                    }
                    if (watchAccounts.isNotEmpty()) {
                        item {
                            BoxBordered(bottom = true) {
                                SectionHeaderColored(title = stringResource(R.string.ManageAccount_WatchAddresses))
                            }
                        }
                        watchAccounts.forEach { account ->
                            item {
                                AccountCellWrapper(account, viewModel, navController)
                            }
                        }
                    }
                    item {
                        VSpacer(32.dp)
                    }
                }
            }

            BottomSearchBar(
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onActiveChange = { isSearchActive = it },
                onSearchQueryChange = { query ->
                    searchQuery = query
                    viewModel.updateFilter(query)
                },
            )
        }
    }
}

@Composable
fun AccountCellWrapper(
    account: AccountViewItem,
    viewModel: ManageAccountsViewModel,
    navController: NavBackStack<HSScreen>
) {
    AccountCell(
        accountViewItem = account,
        onSelect = {
            viewModel.onSelect(account)

            stat(
                page = StatPage.ManageWallets,
                event = StatEvent.Select(StatEntity.Wallet)
            )
        },
        onOptionIconClick = {
            navController.slideFromRight(
                ManageAccountFragment(ManageAccountFragment.Input(account.accountId))
            )

            stat(
                page = StatPage.ManageWallets,
                event = StatEvent.Open(StatPage.ManageWallet)
            )
        }
    )
}

@Composable
fun AccountCell(
    accountViewItem: AccountViewItem,
    onSelect: (AccountViewItem) -> Unit,
    onOptionIconClick: (AccountViewItem) -> Unit
) {
    val icon: Int
    val iconTint: Color
    if (accountViewItem.showAlertIcon) {
        icon = R.drawable.warning_outline_24
        iconTint = ComposeAppTheme.colors.lucian
    } else {
        icon = R.drawable.threedots_24
        iconTint = ComposeAppTheme.colors.leah
    }
    val subtitle = if (accountViewItem.backupRequired) {
        stringResource(id = R.string.ManageAccount_BackupRequired_Title)
            .hs(ComposeAppTheme.colors.lucian)
    } else if (accountViewItem.migrationRequired) {
        stringResource(id = R.string.ManageAccount_MigrationRequired_Title)
            .hs(ComposeAppTheme.colors.lucian)
    } else {
        accountViewItem.subtitle.hs
    }

    BoxBordered(bottom = true) {
        CellPrimary(
            left = {
                CellLeftSelectors(accountViewItem.selected)
            },
            middle = {
                CellMiddleInfo(
                    title = accountViewItem.title.hs,
                    subtitle = subtitle
                )
            },
            right = {
                CellRightControlsIconButton(
                    icon = icon,
                    iconTint = iconTint,
                    onClick = {
                        onOptionIconClick.invoke(accountViewItem)
                    }
                )
            },
            onClick = {
                onSelect.invoke(accountViewItem)
            }
        )
    }
}

