package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.MarketModule.ListType
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardContent
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardHeader
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardItem
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetrics
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsPoint
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.models.ChartPoint
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import kotlin.math.min

class MarketOverviewViewModel(
    private val service: MarketOverviewService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemLiveData = MutableLiveData<MarketOverviewModule.ViewItem?>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String?>()

    private val disposable = CompositeDisposable()

    private var topGainersMarket = TopMarket.Top250
    private var topLosersMarket = TopMarket.Top250

    init {
        service.stateObservable
            .subscribeIO {
                syncState(it)
            }
            .let {
                disposable.add(it)
            }
    }

    private fun syncState(state: DataState<Unit>) {
        when (state) {
            DataState.Loading -> {
                loadingLiveData.postValue(true)
            }
            is DataState.Success -> {
                viewItemLiveData.postValue(getViewItem())
                errorLiveData.postValue(null)
                loadingLiveData.postValue(false)
            }
            is DataState.Error -> {
                viewItemLiveData.postValue(null)
                errorLiveData.postValue(state.error.message ?: state.error.javaClass.simpleName)
                loadingLiveData.postValue(false)
            }
        }
    }

    private fun getViewItem(): MarketOverviewModule.ViewItem? {
        val boardItems = getBoardsData()
        val marketMetrics = getMarketMetrics()
        return if (boardItems.isNotEmpty() && marketMetrics != null) {
            MarketOverviewModule.ViewItem(marketMetrics, boardItems)
        } else {
            null
        }
    }

    fun onToggleTopBoardSize(listType: ListType) {
        when (listType) {
            ListType.TopGainers -> {
                topGainersMarket = topGainersMarket.next()
            }
            ListType.TopLosers -> {
                topLosersMarket = topLosersMarket.next()
            }
        }
        viewItemLiveData.postValue(getViewItem())
    }

    private fun getBoardsData(): List<BoardItem> {
        val marketOverviewItem = service.marketOverviewItem ?: return listOf()

        val topGainersBoard = getBoardItem(ListType.TopGainers, marketOverviewItem.marketItems)
        val topLosersBoard = getBoardItem(ListType.TopLosers, marketOverviewItem.marketItems)

        return listOf(topGainersBoard, topLosersBoard)
    }

    private fun getBoardItem(type: ListType, marketItems: List<MarketItem>): BoardItem {
        val topMarket = when (type) {
            ListType.TopGainers -> topGainersMarket
            ListType.TopLosers -> topLosersMarket
        }
        val topList = marketItems
            .subList(0, min(marketItems.size, topMarket.value))
            .sort(type.sortingField)
            .subList(0, min(marketItems.size, 5))
            .map { MarketViewItem.create(it, type.marketField) }

        val topGainersHeader = BoardHeader(
            getSectionTitle(type),
            getSectionIcon(type),
            getToggleButton(topMarket)
        )
        val topBoardList = BoardContent(topList, type)
        return BoardItem(topGainersHeader, topBoardList, type)
    }

    private fun getMarketMetrics(): MarketMetrics? {
        val marketOverviewItem = service.marketOverviewItem ?: return null

        val marketMetricsItem = marketOverviewItem.marketMetrics
        val btcDominanceFormatted = App.numberFormatter.format(marketMetricsItem.btcDominance, 0, 2, suffix = "%")

        return MarketMetrics(
            totalMarketCap = MetricData(
                formatFiatShortened(marketMetricsItem.marketCap.value, marketMetricsItem.marketCap.currency.symbol),
                marketMetricsItem.marketCapDiff24h,
                getChartData(marketMetricsItem.totalMarketCapPoints),
                MetricsType.TotalMarketCap
            ),
            btcDominance = MetricData(
                btcDominanceFormatted,
                marketMetricsItem.btcDominanceDiff24h,
                getChartData(marketMetricsItem.btcDominancePoints),
                MetricsType.BtcDominance
            ),
            volume24h = MetricData(
                formatFiatShortened(marketMetricsItem.volume24h.value, marketMetricsItem.volume24h.currency.symbol),
                marketMetricsItem.volume24hDiff24h,
                getChartData(marketMetricsItem.volume24Points),
                MetricsType.Volume24h
            ),
            defiCap = MetricData(
                formatFiatShortened(
                    marketMetricsItem.defiMarketCap.value,
                    marketMetricsItem.defiMarketCap.currency.symbol
                ),
                marketMetricsItem.defiMarketCapDiff24h,
                getChartData(marketMetricsItem.defiMarketCapPoints),
                MetricsType.DefiCap
            ),
            defiTvl = MetricData(
                formatFiatShortened(marketMetricsItem.defiTvl.value, marketMetricsItem.defiTvl.currency.symbol),
                marketMetricsItem.defiTvlDiff24h,
                getChartData(marketMetricsItem.defiTvlPoints),
                MetricsType.TvlInDefi
            ),
        )
    }

    private fun getChartData(marketMetricsPoints: List<MarketMetricsPoint>): ChartData {
        val startTimestamp = marketMetricsPoints.first().timestamp
        val endTimestamp = marketMetricsPoints.last().timestamp
        val points = marketMetricsPoints.map { ChartPoint(it.value.toFloat(), null, it.timestamp) }
        return ChartDataFactory.build(points, startTimestamp, endTimestamp, false)
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val (shortenValue, suffix) = App.numberFormatter.shortenValue(value)
        return App.numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + " $suffix"
    }

    private fun getToggleButton(topMarket: TopMarket): MarketListHeaderView.ToggleButton {
        val options = TopMarket.values().map { "${it.value}" }

        return MarketListHeaderView.ToggleButton(
            title = options[topMarket.ordinal],
            indicators = options.mapIndexed { index, _ -> ToggleIndicator(index == topMarket.ordinal) }
        )
    }

    private fun getSectionTitle(type: ListType): Int {
        return when (type) {
            ListType.TopGainers -> R.string.RateList_TopGainers
            ListType.TopLosers -> R.string.RateList_TopLosers
        }
    }

    private fun getSectionIcon(type: ListType): Int {
        return when (type) {
            ListType.TopGainers -> R.drawable.ic_circle_up_20
            ListType.TopLosers -> R.drawable.ic_circle_down_20
        }
    }

    fun onErrorClick() {
        service.refresh()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun refresh() {
        service.refresh()
    }
}
