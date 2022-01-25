package io.horizontalsystems.bankwallet.modules.balance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.shortenedAddress
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceHeaderViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.onboarding.OnboardingScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class BalanceXxxFragment : BaseFragment() {

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
                BalanceXxxScreen(findNavController())
            }
        }
    }
}


@Composable
fun BalanceXxxScreen(navController: NavController) {
    ComposeAppTheme {
        val viewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceXxxModule.AccountsFactory())

        when (val tmpAccount = viewModel.accountViewItem) {
            null -> OnboardingScreen(navController)
            else -> BalanceScreen(navController, tmpAccount)
        }
    }
}

@Composable
fun BalanceScreen(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceXxxViewModel>(factory = BalanceXxxModule.BalanceXxxFactory())

    Column {
        TopAppBar(
            modifier = Modifier.height(56.dp),
            title = {
                Row(
                    modifier = Modifier
                        .clickable {
                            navController.slideFromBottom(
                                R.id.mainFragment_to_manageKeysFragment,
                                ManageAccountsModule.prepareParams(ManageAccountsModule.Mode.Switcher)
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = accountViewItem.name,
                        style = ComposeAppTheme.typography.title3,
                        color = ComposeAppTheme.colors.oz,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_down_24),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            },
            backgroundColor = ComposeAppTheme.colors.tyler,
            elevation = 0.dp
        )

        viewModel.balanceViewItemsWrapper?.let { (headerViewItem, balanceViewItems) ->
            when {
                balanceViewItems.isNotEmpty() -> BalanceItems(
                    headerViewItem,
                    balanceViewItems,
                    viewModel,
                    accountViewItem,
                    navController
                )
                else -> BalanceEmptyItems(navController, accountViewItem)
            }
        }
    }
}

@Composable
fun BalanceItems(
    headerViewItem: BalanceHeaderViewItem,
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceXxxViewModel,
    accountViewItem: AccountViewItem,
    navController: NavController
) {
    val context = LocalContext.current
    TabBalance(
        modifier = Modifier
            .clickable {
//                viewModel.onBalanceClick()
                HudHelper.vibrate(context)
            }
    ) {
        val color = if (headerViewItem.upToDate) {
            ComposeAppTheme.colors.jacob
        } else {
            ComposeAppTheme.colors.yellow50
        }
        Text(
            text = headerViewItem.xBalanceText,
            style = ComposeAppTheme.typography.headline1,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Header(borderTop = true) {
        ButtonSecondaryTransparent(
            title = stringResource(viewModel.sortType.getTitleRes()),
            iconRight = R.drawable.ic_down_arrow_20,
            onClick = {
//                val sortTypes =
//                    listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
//                val selectorItems = sortTypes.map {
//                    SelectorItem(getString(it.getTitleRes()), it == currentSortType)
//                }
//                SelectorDialog
//                    .newInstance(selectorItems, getString(R.string.Balance_Sort_PopupTitle)) { position ->
//                        viewModel.setSortType(sortTypes[position])
//                        scrollToTopAfterUpdate = true
//                    }
//                    .show(parentFragmentManager, "balance_sort_type_selector")

            }
        )

        Spacer(modifier = Modifier.weight(1f))

        val clipboardManager = LocalClipboardManager.current
        accountViewItem.address?.let { address ->
            ButtonSecondaryDefault(
                title = address.shortenedAddress(),
                onClick = {
                    clipboardManager.setText(AnnotatedString(address))
//                    HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
                }
            )
        }
        if (accountViewItem.manageCoinsAllowed) {
            ButtonSecondaryCircle(
                icon = R.drawable.ic_manage_2,
                onClick = {
                    navController.slideFromRight(
                        R.id.mainFragment_to_manageWalletsFragment
                    )
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
    }

//    Wallets(balanceViewItems)

}


@Composable
fun BalanceEmptyItems(navController: NavController, accountViewItem: AccountViewItem) {
    if (accountViewItem.isWatchAccount) {
        NoBalanceWatchAccount()
    } else {
        NoCoins(navController)
    }
}



@Composable
fun NoBalanceWatchAccount() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(ComposeAppTheme.colors.jeremy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_emptywallet_48),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            modifier = Modifier.padding(horizontal = 48.dp),
            text = stringResource(id = R.string.Balance_WatchAccount_NoBalance),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
            textAlign = TextAlign.Center
        )
    }

}

@Composable
fun NoCoins(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(ComposeAppTheme.colors.jeremy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_to_wallet_2_48),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            modifier = Modifier.padding(horizontal = 48.dp),
            text = stringResource(id = R.string.Balance_NoCoinsAlert),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(38.dp))
        ButtonSecondaryDefault(
            title = stringResource(id = R.string.Balance_AddCoins),
            onClick = {
                navController.slideFromRight(R.id.manageWalletsFragment)
            }
        )
    }
}
