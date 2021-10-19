package io.horizontalsystems.bankwallet.modules.market.topcoins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsModule.ViewItemState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketListCoin
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController

class MarketTopCoinsFragment : BaseFragment() {

    private val sortingField by lazy {
        arguments?.getParcelable<SortingField>(sortingFieldKey)
    }
    private val topMarket by lazy {
        arguments?.getParcelable<TopMarket>(topMarketKey)
    }
    private val marketField by lazy {
        arguments?.getParcelable<MarketField>(marketFieldKey)
    }

    val viewModel by viewModels<MarketTopCoinsViewModel> {
        MarketTopCoinsModule.Factory(topMarket, sortingField, marketField)
    }

    @ExperimentalCoilApi
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
                ComposeAppTheme {
                    TopCoinsScreen(
                        viewModel,
                        { findNavController().popBackStack() },
                        { sortingFieldSelect, onSelectSortingField ->
                            onSortingClick(sortingFieldSelect, onSelectSortingField)
                        },
                        { coinUid -> onCoinClick(coinUid) }
                    )
                }
            }
        }
    }

    private fun onSortingClick(sortingFieldSelect: Select<SortingField>, onSelectSortingField: (SortingField) -> Unit) {
        val items = sortingFieldSelect.options.map {
            SelectorItem(getString(it.titleResId), it == sortingFieldSelect.selected)
        }

        SelectorDialog
            .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                val selectedSortingField = sortingFieldSelect.options[position]
                onSelectSortingField(selectedSortingField)
            }
            .show(childFragmentManager, "sorting_field_selector")
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    companion object {
        private const val sortingFieldKey = "sorting_field"
        private const val topMarketKey = "top_market"
        private const val marketFieldKey = "market_field"

        fun prepareParams(sortingField: SortingField, topMarket: TopMarket, marketField: MarketField): Bundle {
            return bundleOf(
                sortingFieldKey to sortingField,
                topMarketKey to topMarket,
                marketFieldKey to marketField
            )
        }
    }

}

@ExperimentalCoilApi
@Composable
fun TopCoinsScreen(
    viewModel: MarketTopCoinsViewModel,
    onCloseButtonClick: () -> Unit,
    onSortMenuClick: (select: Select<SortingField>, onSelect: ((SortingField) -> Unit)) -> Unit,
    onCoinClick: (String) -> Unit,
) {
    val viewItemState by viewModel.viewStateLiveData.observeAsState()
    val header by viewModel.headerLiveData.observeAsState()
    val menu by viewModel.menuLiveData.observeAsState()
    val loading by viewModel.loadingLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState()

    val interactionSource = remember { MutableInteractionSource() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource, onCloseButtonClick)
            header?.let { header ->
                CategoryInfo(header.title, header.description, header.icon)
            }
            menu?.let { menu ->
                CoinListHeader(
                    menu.sortingFieldSelect, viewModel::onSelectSortingField,
                    menu.topMarketSelect, viewModel::onSelectTopMarket,
                    menu.marketFieldSelect, viewModel::onSelectMarketField,
                    onSortMenuClick
                )
            }

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing ?: false || loading ?: false),
                onRefresh = {
                    viewModel.refresh()
                },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        scale = true,
                        backgroundColor = ComposeAppTheme.colors.claude,
                        contentColor = ComposeAppTheme.colors.oz,
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = viewItemState) {
                    is ViewItemState.Error -> {
                        ListErrorView(
                            stringResource(R.string.Market_SyncError)
                        ) {
                            viewModel.onErrorClick()
                        }
                    }
                    is ViewItemState.Data -> {
                        LazyColumn {
                            coinList(state.items, onCoinClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCloseButton(
    interactionSource: MutableInteractionSource,
    onCloseButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onCloseButtonClick.invoke()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "close icon",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}

sealed class ImageSource {
    class Local(@DrawableRes val resId: Int) : ImageSource()
    class Remote(val url: String) : ImageSource()
}

@ExperimentalCoilApi
@Composable
fun CategoryInfo(title: String, description: String, image: ImageSource) {
    Column {
        Row(
            modifier = Modifier.height(108.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = ComposeAppTheme.typography.headline1,
                    color = ComposeAppTheme.colors.oz,
                )
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 6.dp),
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.grey,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Image(
                painter = when (image) {
                    is ImageSource.Local -> painterResource(image.resId)
                    is ImageSource.Remote -> rememberImagePainter(image.url)
                },
                contentDescription = "category image",
                modifier = Modifier
                    .fillMaxHeight()
                    .width(76.dp),
            )
        }
    }
}

@Composable
fun CoinListHeader(
    sortingFieldSelect: Select<SortingField>,
    onSelectSortingField: (SortingField) -> Unit,
    topMarketSelect: Select<TopMarket>?,
    onSelectTopMarket: ((TopMarket) -> Unit)?,
    marketFieldSelect: Select<MarketField>,
    onSelectMarketField: (MarketField) -> Unit,
    onSortMenuClick: (select: Select<SortingField>, onSelect: ((SortingField) -> Unit)) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(sortingFieldSelect.selected.titleResId) {
                    onSortMenuClick(sortingFieldSelect, onSelectSortingField)
                }
            }
            topMarketSelect?.let {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    ButtonSecondaryToggle(select = topMarketSelect, onSelect = onSelectTopMarket ?: {}) //TODO
                }
            }

            Box(modifier = Modifier.padding(start = 8.dp)) {
                ButtonSecondaryToggle(select = marketFieldSelect, onSelect = onSelectMarketField)
            }
        }
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
    }
}

@Composable
private fun SortMenu(titleRes: Int, onClick: () -> Unit) {
    ButtonSecondaryTransparent(
        title = stringResource(titleRes),
        iconRight = R.drawable.ic_down_arrow_20,
        onClick = onClick
    )
}

fun LazyListScope.coinList(
    items: List<MarketTopCoinsModule.MarketViewItem>,
    onCoinClick: (String) -> Unit
) {
    items(items) { item ->
        MarketListCoin(
            item.fullCoin.coin.name,
            item.fullCoin.coin.code,
            item.coinRate,
            item.fullCoin.coin.iconUrl,
            item.fullCoin.iconPlaceholder,
            item.marketDataValue,
            item.rank
        ) { onCoinClick.invoke(item.fullCoin.coin.uid) }
    }
}
