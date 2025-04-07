package cash.p.terminal.modules.market.tvl

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
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.navigation.slideFromRight

import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.ui_compose.CoinFragmentInput
import io.horizontalsystems.chartview.ui.Chart
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.MarketDataValue
import io.horizontalsystems.core.entities.Value
import cash.p.terminal.modules.market.tvl.TvlModule.SelectorDialogState
import cash.p.terminal.modules.market.tvl.TvlModule.TvlDiffType
import cash.p.terminal.ui_compose.components.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import cash.p.terminal.ui.compose.components.DescriptionCard
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.MarketCoinFirstRow
import cash.p.terminal.ui.compose.components.MarketCoinSecondRow
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.SectionItemBorderedRowUniversalClear
import cash.p.terminal.ui.compose.hsRememberLazyListState
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.chartview.rememberAsyncImagePainterWithFallback
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
            val arguments = CoinFragmentInput(coinUid)
            navController.slideFromRight(R.id.coinFragment, arguments)
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

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
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
                }
            ) {
                Crossfade(viewState, label = "") { viewState ->
                    when (viewState) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), tvlViewModel::onErrorClick)
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
                                    Chart(
                                        getSelectedPointCallback = chartViewModel::getSelectedPoint,
                                        uiState = chartViewModel.uiState) {
                                        chartViewModel.onSelectChartInterval(it)
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
                painter = rememberAsyncImagePainterWithFallback(
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
