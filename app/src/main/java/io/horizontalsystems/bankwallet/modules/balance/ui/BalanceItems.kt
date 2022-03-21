package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shortenedAddress
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.*
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppModule
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun BalanceItems(
    headerViewItem: BalanceHeaderViewItem,
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceViewModel,
    accountViewItem: AccountViewItem,
    navController: NavController
) {
    val rateAppViewModel = viewModel<RateAppViewModel>(factory = RateAppModule.Factory())
    DisposableEffect(true) {
        rateAppViewModel.onBalancePageActive()
        onDispose {
            rateAppViewModel.onBalancePageInactive()
        }
    }

    Column {
        val context = LocalContext.current
        TabBalance(
            modifier = Modifier
                .clickable {
                    viewModel.onBalanceClick()
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
            var showSortTypeSelectorDialog by remember { mutableStateOf(false) }

            ButtonSecondaryTransparent(
                title = stringResource(viewModel.sortType.getTitleRes()),
                iconRight = R.drawable.ic_down_arrow_20,
                onClick = {
                    showSortTypeSelectorDialog = true
                }
            )

            if (showSortTypeSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.Balance_Sort_PopupTitle),
                    items = viewModel.sortTypes.map {
                        TabItem(stringResource(it.getTitleRes()), it == viewModel.sortType, it)
                    },
                    onDismissRequest = {
                        showSortTypeSelectorDialog = false
                    },
                    onSelectItem = {
                        viewModel.sortType = it
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            accountViewItem.address?.let { address ->
                val clipboardManager = LocalClipboardManager.current
                val view = LocalView.current
                ButtonSecondaryDefault(
                    title = address.shortenedAddress(),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(address))
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
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

        Wallets(balanceViewItems, viewModel, navController, accountViewItem.id, viewModel.sortType)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Wallets(
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceViewModel,
    navController: NavController,
    accountId: String,
    sortType: BalanceSortType
) {
    var revealedCardId by remember { mutableStateOf<String?>(null) }

    val listState = rememberSaveable(
        accountId,
        sortType,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(viewModel.isRefreshing),
        onRefresh = {
            viewModel.onRefresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(balanceViewItems, key = { item -> item.uid }) { item ->
                if (item.isWatchAccount) {
                    BalanceCard(item, viewModel, navController)
                } else {
                    BalanceCardSwipable(
                        viewItem = item,
                        viewModel = viewModel,
                        navController = navController,
                        modifier = Modifier.animateItemPlacement(
                            spring(
                                stiffness = Spring.StiffnessMedium,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            )
                        ),
                        revealed = revealedCardId == item.uid,
                        onReveal = { id ->
                            if (revealedCardId != id) {
                                revealedCardId = id
                            }
                        },
                        onConceal = {
                            revealedCardId = null
                        },
                    )
                }
            }
        }
    }
}


