package io.horizontalsystems.bankwallet.modules.market.topcoins

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class MarketTopCoinsFragment : BaseComposeFragment() {

    private val sortingField by lazy {
        arguments?.parcelable<SortingField>(sortingFieldKey)
    }
    private val topMarket by lazy {
        arguments?.parcelable<TopMarket>(topMarketKey)
    }
    private val marketField by lazy {
        arguments?.parcelable<MarketField>(marketFieldKey)
    }

    val viewModel by viewModels<MarketTopCoinsViewModel> {
        MarketTopCoinsModule.Factory(topMarket, sortingField, marketField)
    }

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            TopCoinsScreen(
                viewModel,
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
        private const val sortingFieldKey = "sorting_field"
        private const val topMarketKey = "top_market"
        private const val marketFieldKey = "market_field"

        fun prepareParams(
            sortingField: SortingField,
            topMarket: TopMarket,
            marketField: MarketField
        ): Bundle {
            return bundleOf(
                sortingFieldKey to sortingField,
                topMarketKey to topMarket,
                marketFieldKey to marketField
            )
        }
    }

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
            null -> {}
        }
    }
}
