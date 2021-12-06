package io.horizontalsystems.bankwallet.modules.market.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class MarketFavoritesFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel by viewModels<MarketFavoritesViewModel> { MarketFavoritesModule.Factory() }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    MarketFavoritesScreen(viewModel) { onCoinClick(it) }
                }
            }
        }
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)
        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }
}

@Composable
fun MarketFavoritesScreen(
    viewModel: MarketFavoritesViewModel,
    onCoinClick: (String) -> Unit
) {
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val loading by viewModel.loadingLiveData.observeAsState(false)
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val marketFavoritesData by viewModel.viewItemLiveData.observeAsState()
    val sortingFieldDialogState by viewModel.sortingFieldSelectorStateLiveData.observeAsState()
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing ?: false || loading ?: false),
        onRefresh = {
            viewModel.refresh()
        }
    ) {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            when (viewState) {
                is ViewState.Error -> {
                    ListErrorView(
                        stringResource(R.string.Market_SyncError)
                    ) {
                        viewModel.onErrorClick()
                    }
                }
                ViewState.Success -> {
                    marketFavoritesData?.let { data ->
                        if (data.marketItems.isEmpty()) {
                            NoFavorites()
                        } else {
                            MarketFavoritesMenu(
                                data.sortingFieldSelect,
                                data.marketFieldSelect,
                                viewModel::onClickSortingField,
                                viewModel::onSelectMarketField
                            )
                            CoinList(data.marketItems, scrollToTopAfterUpdate, onCoinClick)
                            if (scrollToTopAfterUpdate) {
                                scrollToTopAfterUpdate = false
                            }
                        }
                    }
                }
            }
        }
    }

    when (val option = sortingFieldDialogState) {
        is MarketFavoritesModule.SelectorDialogState.Opened -> {
            AlertGroup(
                R.string.Market_Sort_PopupTitle,
                option.select,
                { selected ->
                    scrollToTopAfterUpdate = true
                    viewModel.onSelectSortingField(selected)
                },
                { viewModel.onSortingFieldDialogDismiss() }
            )
        }
    }
}

@Composable
fun MarketFavoritesMenu(
    sortingFieldSelect: Select<SortingField>,
    marketFieldSelect: Select<MarketField>,
    onClickSortingField: () -> Unit,
    onSelectMarketField: (MarketField) -> Unit
) {

    Header(borderTop = true, borderBottom = true) {
        Box(modifier = Modifier.weight(1f)) {
            SortMenu(sortingFieldSelect.selected.title, onClickSortingField)
        }
        ButtonSecondaryToggle(
            modifier = Modifier.padding(end = 16.dp),
            select = marketFieldSelect,
            onSelect = onSelectMarketField
        )
    }
}

@Composable
fun NoFavorites() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier
                .size(48.dp),
            painter = painterResource(id = R.drawable.ic_rate_24),
            contentDescription = stringResource(id = R.string.Market_Tab_Watchlist_EmptyList),
            colorFilter = ColorFilter.tint(ComposeAppTheme.colors.grey)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 48.dp),
            text = stringResource(id = R.string.Market_Tab_Watchlist_EmptyList),
            textAlign = TextAlign.Center,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
        )
    }
}
