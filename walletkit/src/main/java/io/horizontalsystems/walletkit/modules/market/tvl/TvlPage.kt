package io.horizontalsystems.walletkit.modules.market.tvl

import android.view.View
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.coin.CoinPage
import io.horizontalsystems.walletkit.modules.coin.overview.ui.Chart
import io.horizontalsystems.walletkit.modules.coin.overview.ui.Loading
import io.horizontalsystems.walletkit.modules.market.MarketDataValue
import io.horizontalsystems.walletkit.modules.market.Value
import io.horizontalsystems.walletkit.modules.market.tvl.TvlModule.SelectorDialogState
import io.horizontalsystems.walletkit.modules.market.tvl.TvlModule.TvlDiffType
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.HSSwipeRefresh
import io.horizontalsystems.walletkit.ui.compose.Select
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.AlertGroup
import io.horizontalsystems.walletkit.ui.compose.components.DescriptionCard
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HeaderSorting
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.ListErrorView
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.marketDataValueComponent
import io.horizontalsystems.walletkit.ui.compose.hsRememberLazyListState
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.ImageType
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.controls.HSDropdownButton
import io.horizontalsystems.walletkit.uiv3.components.controls.HSIconButton
import kotlinx.serialization.Serializable

@Serializable
data object TvlPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val vmFactory = remember { TvlModule.Factory() }
        val tvlChartViewModel = viewModel<TvlChartViewModel>(factory = vmFactory)
        val viewModel = viewModel<TvlViewModel>(factory = vmFactory)

        val view = LocalView.current
        TvlScreen(viewModel, tvlChartViewModel, navigation) {
            onCoinClick(
                it,
                navigation,
                view
            )
        }
    }

    private fun onCoinClick(
        coinUid: String?,
        navigation: HSNavigation,
        view: View
    ) {
        if (coinUid != null) {
            val arguments = CoinPage.Input(coinUid)
            navigation.slideFromRight(CoinPage(arguments))

            stat(page = StatPage.GlobalMetricsTvlInDefi, event = StatEvent.OpenCoin(coinUid))
        } else {
            HudHelper.showWarningMessage(view, R.string.MarketGlobalMetrics_NoCoin)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvlScreen(
    tvlViewModel: TvlViewModel,
    chartViewModel: TvlChartViewModel,
    navigation: HSNavigation,
    onCoinClick: (String?) -> Unit
) {
    val itemsViewState by tvlViewModel.viewStateLiveData.observeAsState()
    val viewState = itemsViewState?.merge(chartViewModel.uiState.viewState)
    val tvlData by tvlViewModel.tvlLiveData.observeAsState()
    val tvlDiffType by tvlViewModel.tvlDiffTypeLiveData.observeAsState()
    val isRefreshing by tvlViewModel.isRefreshingLiveData.observeAsState(false)
    val chainSelectorDialogState by tvlViewModel.chainSelectorDialogStateLiveData.observeAsState(
        SelectorDialogState.Closed
    )

    HSScaffold(
        title = "",
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = {
                    navigation.removeLastOrNull()
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ComposeAppTheme.colors.lawrence),
                                contentPadding = PaddingValues(bottom = 32.dp),
                            ) {
                                item {
                                    tvlViewModel.header.let { header ->
                                        DescriptionCard(
                                            header.title,
                                            header.description,
                                            header.icon
                                        )
                                    }
                                }
                                item {
                                    Chart(
                                        modifier = Modifier.background(ComposeAppTheme.colors.tyler),
                                        chartViewModel = chartViewModel
                                    ) {
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
                                                    MarketDataValue.Diff(Value.Percent(item.tvlChangePercent))
                                                }

                                                TvlDiffType.Currency -> item.tvlChangeAmount?.let {
                                                    MarketDataValue.Diff(Value.Currency(item.tvlChangeAmount))
                                                }

                                                else -> null
                                            },
                                            item.rank
                                        ) { onCoinClick(item.coinUid) }
                                        HsDivider()
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
                            title = stringResource(R.string.MarketGlobalMetrics_ChainSelectorTitle),
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
    HeaderSorting(
        borderBottom = true,
        borderTop = true,
        backgroundColor = ComposeAppTheme.colors.lawrence
    ) {
        HSpacer(16.dp)
        HSDropdownButton(
            variant = ButtonVariant.Secondary,
            onClick = onClickChainSelector,
            title = chainSelect.selected.title.getString(),
        )
        HSpacer(8.dp)
        HSButton(
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Small,
            title = stringResource(R.string.Market_TVL),
            icon = painterResource(if (sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20),
            onClick = onToggleSortType
        )
        tvlDiffType?.let {
            HSpacer(8.dp)
            HSIconButton(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                icon = painterResource(if (tvlDiffType == TvlDiffType.Percent) R.drawable.ic_percent_20 else R.drawable.ic_usd_20),
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
    CellPrimary(
        left = {
            CellLeftImage(
                type = ImageType.Rectangle,
                size = 32,
                painter = rememberAsyncImagePainter(
                    model = iconUrl,
                    error = iconPlaceholder?.let { alternativeUrl ->
                        rememberAsyncImagePainter(
                            model = alternativeUrl,
                            error = painterResource(R.drawable.ic_platform_placeholder_24)
                        )
                    } ?: painterResource(R.drawable.ic_platform_placeholder_24)
                ),
            )
        },
        middle = {
            CellMiddleInfo(
                title = name.hs,
                subtitle = chain.getString().hs,
                subtitleBadge = label?.hs,
            )
        },
        right = {
            CellRightInfo(
                title = tvl.getFormattedShort().hs,
                subtitle = marketDataValueComponent(marketDataValue)
            )
        },
        onClick = onClick
    )
}
