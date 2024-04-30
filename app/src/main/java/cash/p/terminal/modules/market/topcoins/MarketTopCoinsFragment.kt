package cash.p.terminal.modules.market.topcoins

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
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.ButtonSecondaryToggle
import cash.p.terminal.ui.compose.components.CoinList
import cash.p.terminal.ui.compose.components.DescriptionCard
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.SortMenu
import cash.p.terminal.ui.compose.components.TopCloseButton
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
        val arguments = CoinFragment.Input(coinUid, "market_overview_top_coins")

        navController.slideFromRight(R.id.coinFragment, arguments)
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
                                    onAddFavorite = { uid -> viewModel.onAddFavorite(uid) },
                                    onRemoveFavorite = { uid -> viewModel.onRemoveFavorite(uid) },
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
                                                                    }
                                                                )
                                                            }
                                                        }

                                                        Box(modifier = Modifier.padding(start = 8.dp)) {
                                                            ButtonSecondaryToggle(
                                                                select = menu.marketFieldSelect,
                                                                onSelect = viewModel::onSelectMarketField
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
