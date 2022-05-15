package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.*
import kotlinx.coroutines.launch

class CoinMarketsFragment : BaseFragment() {

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinMarketsViewModel> {
        CoinMarketsModule.Factory(coinViewModel.fullCoin)
    }

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
                    CoinMarketsScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun CoinMarketsScreen(
    viewModel: CoinMarketsViewModel,
) {
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val viewItemState by viewModel.viewStateLiveData.observeAsState()
    val viewItems by viewModel.viewItemsLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Crossfade(viewItemState) { viewItemState ->
            when (viewItemState) {
                is ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                is ViewState.Success -> {
                    viewItems?.let { items ->
                        Column {
                            if (items.isEmpty()) {
                                ListEmptyView(
                                    text = stringResource(R.string.CoinPage_NoDataAvailable),
                                    icon = R.drawable.ic_no_data
                                )
                            } else {
                                CoinMarketsMenu(
                                    viewModel.sortingType,
                                    viewModel.volumeMenu,
                                    {
                                        viewModel.toggleSortType(it)
                                        scrollToTopAfterUpdate = true
                                    },
                                    { viewModel.toggleVolumeType(it) }
                                )
                                CoinMarketList(items, scrollToTopAfterUpdate)
                                if (scrollToTopAfterUpdate) {
                                    scrollToTopAfterUpdate = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoinMarketsMenu(
    menuSorting: SortType,
    menuVolumeType: Select<CoinMarketsModule.VolumeMenuType>,
    onToggleSortType: (SortType) -> Unit,
    onToggleVolumeType: (CoinMarketsModule.VolumeMenuType) -> Unit
) {

    var sortingType by remember { mutableStateOf(menuSorting) }
    var volumeType by remember { mutableStateOf(menuVolumeType) }

    Header(borderTop = true, borderBottom = true) {
        ButtonSecondaryCircle(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            icon = if (sortingType == SortType.HighestVolume) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
            onClick = {
                val next = sortingType.next()
                onToggleSortType(next)
                sortingType = next
            }
        )
        ButtonSecondaryToggle(
            modifier = Modifier.padding(end = 16.dp),
            select = volumeType,
            onSelect = {
                onToggleVolumeType.invoke(it)
                volumeType = Select(it, volumeType.options)
            }
        )
    }
}

@Composable
fun CoinMarketList(
    items: List<MarketTickerViewItem>,
    scrollToTop: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(items) { item ->
            CoinMarketCell(
                item.market,
                item.pair,
                item.marketImageUrl ?: "",
                item.rate,
                MarketDataValue.Volume(item.volume),
            )
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (scrollToTop) {
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
        }
    }
}

@Composable
fun CoinMarketCell(
    name: String,
    subtitle: String,
    iconUrl: String,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
) {
    MultilineClear(
        borderBottom = true
    ) {
        Image(
            painter = rememberImagePainter(
                data = iconUrl,
                builder = { error(R.drawable.coin_placeholder) }
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(name, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(subtitle, marketDataValue, null)
        }
    }
}
