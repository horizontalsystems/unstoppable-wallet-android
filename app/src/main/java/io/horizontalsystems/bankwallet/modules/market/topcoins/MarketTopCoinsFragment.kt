package io.horizontalsystems.bankwallet.modules.market.topcoins

import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statField
import io.horizontalsystems.bankwallet.core.stats.statMarketTop
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.CoinList
import io.horizontalsystems.bankwallet.ui.compose.components.DescriptionCard
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.SortMenu
import io.horizontalsystems.bankwallet.ui.compose.components.TopCloseButton
import kotlinx.parcelize.Parcelize

class MarketTopCoinsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        val sortingField = input?.sortingField
        val topMarket = input?.topMarket
        val marketField = input?.marketField

        val viewModel = viewModel<MarketTopCoinsViewModel>(
            factory = MarketTopCoinsModule.Factory(topMarket, sortingField, marketField)
        )

        TopCoinsScreen(
            viewModel,
            { navController.popBackStack() },
            { coinUid -> onCoinClick(coinUid, navController) }
        )
    }

    private fun onCoinClick(coinUid: String, navController: NavController) {
        val arguments = CoinFragment.Input(coinUid)

        navController.slideFromRight(R.id.coinFragment, arguments)

        stat(page = StatPage.TopCoins, event = StatEvent.OpenCoin(coinUid))
    }

    @Parcelize
    data class Input(
        val sortingField: SortingField,
        val topMarket: TopMarket,
        val marketField: MarketField
    ) : Parcelable
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopCoinsScreen(
    viewModel: MarketTopCoinsViewModel,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
) {
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val viewItems by viewModel.viewItemsLiveData.observeAsState()
    val header by viewModel.headerLiveData.observeAsState()
    val menu by viewModel.menuLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val selectorDialogState by viewModel.selectorDialogStateLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(onCloseButtonClick)

            HSSwipeRefresh(
                refreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewState) { state ->
                    when (state) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        }

                        ViewState.Success -> {
                            viewItems?.let {
                                CoinList(
                                    items = it,
                                    scrollToTop = scrollToTopAfterUpdate,
                                    onAddFavorite = { uid ->
                                        viewModel.onAddFavorite(uid)

                                        stat(page = StatPage.TopCoins, event = StatEvent.AddToWatchlist(uid))
                                    },
                                    onRemoveFavorite = { uid ->
                                        viewModel.onRemoveFavorite(uid)

                                        stat(page = StatPage.TopCoins, event = StatEvent.RemoveFromWatchlist(uid))
                                    },
                                    onCoinClick = onCoinClick,
                                    preItems = {
                                        header?.let { header ->
                                            item {
                                                DescriptionCard(header.title, header.description, header.icon)
                                            }
                                        }

                                        menu?.let { menu ->
                                            stickyHeader {
                                                HeaderSorting(
                                                    borderTop = true,
                                                    borderBottom = true
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(end = 16.dp)
                                                            .height(44.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            SortMenu(
                                                                titleRes = menu.sortingFieldSelect.selected.titleResId,
                                                                onClick = viewModel::showSelectorMenu
                                                            )
                                                        }

                                                        menu.topMarketSelect?.let {
                                                            Box(modifier = Modifier.padding(start = 8.dp)) {
                                                                ButtonSecondaryToggle(
                                                                    select = menu.topMarketSelect,
                                                                    onSelect = { topMarket ->
                                                                        scrollToTopAfterUpdate =
                                                                            true
                                                                        viewModel.onSelectTopMarket(
                                                                            topMarket
                                                                        )

                                                                        stat(page = StatPage.TopCoins, event = StatEvent.SwitchMarketTop(topMarket.statMarketTop))
                                                                    }
                                                                )
                                                            }
                                                        }

                                                        Box(modifier = Modifier.padding(start = 8.dp)) {
                                                            ButtonSecondaryToggle(
                                                                select = menu.marketFieldSelect,
                                                                onSelect = {
                                                                    viewModel.onSelectMarketField(it)

                                                                    stat(page = StatPage.TopCoins, event = StatEvent.SwitchField(it.statField))
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                if (scrollToTopAfterUpdate) {
                                    scrollToTopAfterUpdate = false
                                }
                            }
                        }

                        null -> {}
                    }
                }
            }
        }
        //Dialog
        when (val option = selectorDialogState) {
            is SelectorDialogState.Opened -> {
                AlertGroup(
                    R.string.Market_Sort_PopupTitle,
                    option.select,
                    { selected ->
                        scrollToTopAfterUpdate = true
                        viewModel.onSelectSortingField(selected)

                        stat(page = StatPage.TopCoins, event = StatEvent.SwitchSortType(selected.statSortType))
                    },
                    { viewModel.onSelectorDialogDismiss() }
                )
            }

            SelectorDialogState.Closed,
            null -> {
            }
        }
    }
}
