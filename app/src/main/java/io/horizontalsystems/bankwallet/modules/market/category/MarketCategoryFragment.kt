package io.horizontalsystems.bankwallet.modules.market.category

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class MarketCategoryFragment : BaseComposeFragment() {

    private val factory by lazy {
        MarketCategoryModule.Factory(arguments?.parcelable(categoryKey)!!)
    }

    private val chartViewModel by viewModels<ChartViewModel> { factory }

    private val viewModel by viewModels<MarketCategoryViewModel> { factory }

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            CategoryScreen(
                viewModel,
                chartViewModel,
                { findNavController().popBackStack() },
                { coinUid -> onCoinClick(coinUid) }
            )
        }
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().slideFromRight(R.id.coinFragment, arguments)
    }

    companion object {
        const val categoryKey = "coin_category"
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    viewModel: MarketCategoryViewModel,
    chartViewModel: ChartViewModel,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
) {
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val viewItemState by viewModel.viewStateLiveData.observeAsState(ViewState.Loading)
    val viewItems by viewModel.viewItemsLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val selectorDialogState by viewModel.selectorDialogStateLiveData.observeAsState()

    val interactionSource = remember { MutableInteractionSource() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource, onCloseButtonClick)

            HSSwipeRefresh(
                refreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewItemState) { state ->
                    when (state) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        }

                        ViewState.Success -> {
                            viewItems?.let {
                                val header by viewModel.headerLiveData.observeAsState()
                                val menu by viewModel.menuLiveData.observeAsState()

                                CoinList(
                                    items = it,
                                    scrollToTop = scrollToTopAfterUpdate,
                                    onAddFavorite = { uid -> viewModel.onAddFavorite(uid) },
                                    onRemoveFavorite = { uid -> viewModel.onRemoveFavorite(uid) },
                                    onCoinClick = onCoinClick,
                                    preItems = {
                                        header?.let {
                                            item {
                                                DescriptionCard(it.title, it.description, it.icon)
                                            }
                                        }
                                        item {
                                            Chart(chartViewModel = chartViewModel)
                                        }
                                        menu?.let {
                                            stickyHeader {
                                                HeaderSorting(borderTop = true, borderBottom = true) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        SortMenu(
                                                            it.sortingFieldSelect.selected.titleResId,
                                                            viewModel::showSelectorMenu
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier.padding(
                                                            start = 8.dp,
                                                            end = 16.dp
                                                        )
                                                    ) {
                                                        ButtonSecondaryToggle(
                                                            select = it.marketFieldSelect,
                                                            onSelect = viewModel::onSelectMarketField
                                                        )
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

