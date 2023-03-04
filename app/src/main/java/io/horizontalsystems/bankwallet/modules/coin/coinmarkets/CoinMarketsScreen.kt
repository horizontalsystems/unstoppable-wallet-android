package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.launch

@Composable
fun CoinMarketsScreen(
    fullCoin: FullCoin
) {
    val viewModel = viewModel<CoinMarketsViewModel>(factory = CoinMarketsModule.Factory(fullCoin))

    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val viewItemState by viewModel.viewStateLiveData.observeAsState()
    val viewItems by viewModel.viewItemsLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Crossfade(viewItemState) { viewItemState ->
            when (viewItemState) {
                ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                ViewState.Success -> {
                    viewItems?.let { items ->
                        Column(modifier = Modifier.fillMaxSize()) {
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
                null -> {}
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

    HeaderSorting(borderTop = true, borderBottom = true) {
        ButtonSecondaryCircle(
            modifier = Modifier
                .padding(start = 16.dp),
            icon = if (sortingType == SortType.HighestVolume) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
            onClick = {
                val next = sortingType.next()
                onToggleSortType(next)
                sortingType = next
            }
        )
        Spacer(Modifier.weight(1f))
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
                item.tradeUrl
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
    tradeUrl: String?,
) {
    val context = LocalContext.current
    SectionItemBorderedRowUniversalClear(
        onClick = tradeUrl?.let {
            { LinkHelper.openLinkInAppBrowser(context, it) }
        },
        borderBottom = true
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = iconUrl,
                error = painterResource(R.drawable.ic_platform_placeholder_24)
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp)),
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
