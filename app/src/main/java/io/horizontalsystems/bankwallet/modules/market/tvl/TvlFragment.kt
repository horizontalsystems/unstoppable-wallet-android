package io.horizontalsystems.bankwallet.modules.market.tvl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfo
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeader
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.TvlDiffType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class TvlFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val viewModel by viewModels<TvlViewModel> { TvlModule.Factory() }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    TvlScreen(viewModel) { onCoinClick(it) }
                }
            }
        }
    }

    private fun onCoinClick(coinUid: String?) {
        if (coinUid != null) {
            val arguments = CoinFragment.prepareParams(coinUid)
            findNavController().navigate(R.id.coinFragment, arguments, navOptions())
        } else {
            HudHelper.showWarningMessage(requireView(), R.string.MarketGlobalMetrics_CoinNotSupported)
        }
    }

    @Composable
    private fun TvlScreen(
        viewModel: TvlViewModel,
        onCoinClick: (String?) -> Unit
    ) {
        val viewState by viewModel.viewStateLiveData.observeAsState()
        val chartData by viewModel.chartLiveData.observeAsState()
        val tvlData by viewModel.tvlLiveData.observeAsState()
        val tvlDiffType by viewModel.tvlDiffTypeLiveData.observeAsState()
        val loading by viewModel.loadingLiveData.observeAsState(false)
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
        val chainSelectorDialogState by viewModel.chainSelectorDialogStateLiveData.observeAsState(SelectorDialogState.Closed)

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.MarketGlobalMetrics_TvlInDefi),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            findNavController().popBackStack()
                        }
                    )
                )
            )

            HSSwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing || loading),
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                when (viewState) {
                    is ViewState.Error -> {
                        ListErrorView(
                            stringResource(R.string.Market_SyncError)
                        ) {
                            viewModel.onErrorClick()
                        }
                    }
                    ViewState.Success -> {
                        LazyColumn {
                            chartData?.let { chartData ->
                                item {
                                    ChartInfoHeader(chartData.subtitle)

                                    ChartInfo(
                                        CoinChartAdapter.ViewItemWrapper(chartData.chartInfoData),
                                        chartData.currency,
                                        CoinChartAdapter.ChartViewType.MarketMetricChart,
                                        listOf(
                                            Pair(ChartView.ChartType.DAILY, R.string.CoinPage_TimeDuration_Day),
                                            Pair(ChartView.ChartType.WEEKLY, R.string.CoinPage_TimeDuration_Week),
                                            Pair(ChartView.ChartType.MONTHLY, R.string.CoinPage_TimeDuration_Month)
                                        ),
                                        object : CoinChartAdapter.Listener {
                                            override fun onChartTouchDown() = Unit

                                            override fun onChartTouchUp() = Unit

                                            override fun onTabSelect(chartType: ChartType) {
                                                viewModel.onSelectChartType(chartType)
                                            }
                                        })
                                }
                            }

                            tvlData?.let { tvlData ->
                                item {
                                    TvlMenu(
                                        tvlData.chainSelect,
                                        tvlData.sortDescending,
                                        tvlDiffType,
                                        viewModel::onClickChainSelector,
                                        viewModel::onToggleSortType,
                                        viewModel::onToggleTvlDiffType
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
                }
                // chain selector dialog
                when (val option = chainSelectorDialogState) {
                    is SelectorDialogState.Opened -> {
                        AlertGroup(
                            R.string.MarketGlobalMetrics_ChainSelectorTitle,
                            option.select,
                            viewModel::onSelectChain,
                            viewModel::onChainSelectorDialogDismiss
                        )
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
        Header(borderBottom = true) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(chainSelect.selected.title) {
                    onClickChainSelector()
                }
            }
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 16.dp),
                icon = if (sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
                onClick = { onToggleSortType() }
            )
            tvlDiffType?.let {
                ButtonSecondaryCircle(
                    modifier = Modifier.padding(end = 16.dp),
                    icon = if (tvlDiffType == TvlDiffType.Percent) R.drawable.ic_percent_20 else R.drawable.ic_usd_20,
                    onClick = { onToggleTvlDiffType() }
                )
            }
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
        MultilineClear(
            onClick = onClick,
            borderBottom = true
        ) {
            CoinImage(
                iconUrl = iconUrl,
                placeholder = iconPlaceholder,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                MarketCoinFirstRow(name, formatCurrencyValueAsShortened(tvl))
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(chain.getString(), marketDataValue, label)
            }
        }
    }

}
