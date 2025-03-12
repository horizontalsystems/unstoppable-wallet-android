package io.horizontalsystems.bankwallet.modules.market.tvl

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.TvlDiffType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.DescriptionCard
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinSecondRow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.hsRememberLazyListState
import io.horizontalsystems.core.helpers.HudHelper

class TvlFragment : BaseComposeFragment() {

    private val vmFactory by lazy { TvlModule.Factory() }
    private val tvlChartViewModel by viewModels<TvlChartViewModel> { vmFactory }
    private val viewModel by viewModels<TvlViewModel> { vmFactory }

    @Composable
    override fun GetContent(navController: NavController) {
        TvlScreen(viewModel, tvlChartViewModel, navController) { onCoinClick(it, navController) }
    }

    private fun onCoinClick(coinUid: String?, navController: NavController) {
        if (coinUid != null) {
            val arguments = CoinFragment.Input(coinUid)
            navController.slideFromRight(R.id.coinFragment, arguments)

            stat(page = StatPage.GlobalMetricsTvlInDefi, event = StatEvent.OpenCoin(coinUid))
        } else {
            HudHelper.showWarningMessage(requireView(), R.string.MarketGlobalMetrics_NoCoin)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun TvlScreen(
        tvlViewModel: TvlViewModel,
        chartViewModel: TvlChartViewModel,
        navController: NavController,
        onCoinClick: (String?) -> Unit
    ) {
        val itemsViewState by tvlViewModel.viewStateLiveData.observeAsState()
        val viewState = itemsViewState?.merge(chartViewModel.uiState.viewState)
        val tvlData by tvlViewModel.tvlLiveData.observeAsState()
        val tvlDiffType by tvlViewModel.tvlDiffTypeLiveData.observeAsState()
        val isRefreshing by tvlViewModel.isRefreshingLiveData.observeAsState(false)
        val chainSelectorDialogState by tvlViewModel.chainSelectorDialogStateLiveData.observeAsState(SelectorDialogState.Closed)

        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
                .navigationBarsPadding()
        ) {
            AppBar(
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            HSSwipeRefresh(
                refreshing = isRefreshing,
                onRefresh = {
                    tvlViewModel.refresh()
                    chartViewModel.refresh()
                }
            ) {
                Crossfade(viewState, label = "") { viewState ->
                    when (viewState) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(
                                errorText = stringResource(R.string.SyncError),
                                onClick = {
                                    tvlViewModel.onErrorClick()
                                    chartViewModel.refresh()
                                }
                            )
                        }

                        ViewState.Success -> {
                            val listState = hsRememberLazyListState(
                                2,
                                tvlData?.chainSelect?.selected,
                                tvlData?.sortDescending
                            )

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 32.dp),
                            ) {
                                item {
                                    tvlViewModel.header.let { header ->
                                        DescriptionCard(header.title, header.description, header.icon)
                                    }
                                }
                                item {
                                    Chart(chartViewModel = chartViewModel) {
                                        tvlViewModel.onSelectChartInterval(it)
                                    }
                                }

                                tvlData?.let { tvlData ->
                                    stickyHeader {
                                        TvlMenu(
                                            tvlData.chainSelect,
                                            tvlData.sortDescending,
                                            tvlDiffType,
                                            tvlViewModel::onClickChainSelector,
                                            tvlViewModel::onToggleSortType,
                                            tvlViewModel::onToggleTvlDiffType
                                        )
                                    }

                                    items(tvlData.coinTvlViewItems) { item ->
                                        DefiMarket(
                                            item.name,
                                            item.chain,
                                            item.iconUrl,
                                            item.iconPlaceholder,
                                            item.tvl,
                                            when (tvlDiffType) {
                                                TvlDiffType.Percent -> item.tvlChangePercent?.let {
                                                    MarketDataValue.DiffNew(Value.Percent(item.tvlChangePercent))
                                                }

                                                TvlDiffType.Currency -> item.tvlChangeAmount?.let {
                                                    MarketDataValue.DiffNew(Value.Currency(item.tvlChangeAmount))
                                                }

                                                else -> null
                                            },
                                            item.rank
                                        ) { onCoinClick(item.coinUid) }
                                    }
                                }
                            }
                        }

                        null -> {}
                    }
                }
                // chain selector dialog
                when (val option = chainSelectorDialogState) {
                    is SelectorDialogState.Opened -> {
                        AlertGroup(
                            title = R.string.MarketGlobalMetrics_ChainSelectorTitle,
                            select = option.select,
                            onSelect = {
                                chartViewModel.onSelectChain(it)
                                tvlViewModel.onSelectChain(it)
                            },
                            onDismiss = tvlViewModel::onChainSelectorDialogDismiss
                        )
                    }

                    SelectorDialogState.Closed -> {}
                }
            }
        }
    }

    @Composable
    private fun TvlMenu(
        chainSelect: Select<TvlModule.Chain>,
        sortDescending: Boolean,
        tvlDiffType: TvlDiffType?,
        onClickChainSelector: () -> Unit,
        onToggleSortType: () -> Unit,
        onToggleTvlDiffType: () -> Unit
    ) {
        HeaderSorting(borderBottom = true, borderTop = true) {
            HSpacer(16.dp)
            ButtonSecondaryWithIcon(
                modifier = Modifier.height(28.dp),
                onClick = onClickChainSelector,
                title =chainSelect.selected.title.getString(),
                iconRight = painterResource(R.drawable.ic_down_arrow_20),
            )
            HSpacer(8.dp)
            ButtonSecondaryWithIcon(
                title = stringResource(R.string.Market_TVL),
                iconRight = painterResource(
                    if (sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20
                ),
                onClick = onToggleSortType
            )
            tvlDiffType?.let {
                HSpacer(8.dp)
                ButtonSecondaryCircle(
                    modifier = Modifier.padding(end = 16.dp),
                    icon = if (tvlDiffType == TvlDiffType.Percent) R.drawable.ic_percent_20 else R.drawable.ic_usd_20,
                    onClick = { onToggleTvlDiffType() }
                )
            }
            HSpacer(width = 16.dp)
        }
    }

    @Composable
    private fun DefiMarket(
        name: String,
        chain: TranslatableString,
        iconUrl: String,
        iconPlaceholder: Int?,
        tvl: CurrencyValue,
        marketDataValue: MarketDataValue?,
        label: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        SectionItemBorderedRowUniversalClear(
            onClick = onClick,
            borderBottom = true
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = iconUrl,
                    error = painterResource(
                        iconPlaceholder ?: R.drawable.ic_platform_placeholder_24
                    )
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
                MarketCoinFirstRow(name, tvl.getFormattedShort())
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(chain.getString(), marketDataValue, label)
            }
        }
    }

}
