package io.horizontalsystems.bankwallet.modules.manageaccounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.ActionViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class ManageAccountsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ManageAccountsScreen(findNavController(), arguments?.getParcelable(ManageAccountsModule.MODE)!!)
            }
        }
    }
}

@Composable
fun ManageAccountsScreen(navController: NavController, mode: ManageAccountsModule.Mode) {
    val viewModel = viewModel<ManageAccountsViewModel>(factory = ManageAccountsModule.Factory(mode))

    val viewItems by viewModel.viewItemsLiveData.observeAsState()
    val finish by viewModel.finishLiveEvent.observeAsState()
    val isCloseButtonVisible = viewModel.isCloseButtonVisible

    if (finish != null) {
        navController.popBackStack()
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            var menuItems: List<MenuItem> = listOf()
            var navigationIcon: @Composable (() -> Unit)? = null

            if (isCloseButtonVisible) {
                menuItems = listOf(MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = { navController.popBackStack() }
                ))
            } else {
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            }
            AppBar(
                title = TranslatableString.ResString(R.string.ManageAccounts_Title),
                navigationIcon = navigationIcon,
                menuItems = menuItems
            )

            LazyColumn(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))

                    viewItems?.let { (regularAccounts, watchAccounts) ->
                        if (regularAccounts.isNotEmpty()) {
                            AccountsSection(regularAccounts, viewModel, navController)
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (watchAccounts.isNotEmpty()) {
                            AccountsSection(watchAccounts, viewModel, navController)
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    val actions = listOf(
                        ActionViewItem(R.drawable.ic_plus, R.string.ManageAccounts_CreateNewWallet) {
                            navController.slideFromRight(R.id.createAccountFragment)
                        },
                        ActionViewItem(R.drawable.ic_download_20, R.string.ManageAccounts_ImportWallet) {
                            navController.slideFromRight(R.id.restoreMnemonicFragment)
                        },
                        ActionViewItem(R.drawable.ic_eye_2_20, R.string.ManageAccounts_WatchAddress) {
                            navController.slideFromRight(R.id.watchAddressFragment)
                        }
                    )
                    CellSingleLineLawrenceSection(actions) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = it.callback),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(id = it.icon),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                            Text(
                                text = stringResource(id = it.title),
                                style = ComposeAppTheme.typography.body,
                                color = ComposeAppTheme.colors.jacob
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        text = stringResource(id = R.string.ManageAccounts_Hint),
                        style = ComposeAppTheme.typography.subhead2,
                        color = ComposeAppTheme.colors.grey
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountsSection(accounts: List<AccountViewItem>, viewModel: ManageAccountsViewModel, navController: NavController) {
    CellMultilineLawrenceSection(items = accounts) { accountViewItem ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    viewModel.onSelect(accountViewItem)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (accountViewItem.selected) {
                Icon(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    painter = painterResource(id = R.drawable.ic_radion),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    painter = painterResource(id = R.drawable.ic_radioff),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountViewItem.title,
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.leah
                )
                Text(
                    text = accountViewItem.subtitle,
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.grey
                )
            }
            if (accountViewItem.alert) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_attention_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.lucian
                )
            }
            if (accountViewItem.alert && accountViewItem.isWatchAccount) {
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (accountViewItem.isWatchAccount) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_eye_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
            Icon(
                modifier = Modifier
                    .clickable {
                        navController.slideFromRight(
                            R.id.manageAccountFragment,
                            ManageAccountModule.prepareParams(accountViewItem.accountId)
                        )
                    }
                    .padding(12.dp),
                painter = painterResource(id = R.drawable.ic_more2_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
