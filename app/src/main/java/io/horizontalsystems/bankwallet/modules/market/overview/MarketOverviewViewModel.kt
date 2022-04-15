package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.market.MarketModule.ListType
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.Board
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardHeader
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetrics
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsItem
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsPoint
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopNftCollectionViewItem
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopNftCollectionsBoard
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopSectorsBoard
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.modules.nft.TopNftCollection
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.chartview.models.ChartPoint
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MarketOverviewViewModel(
    private val service: MarketOverviewService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private data class OverviewItemsWrapper(
        val topGainers: Result<List<MarketItem>>,
        val topLosers: Result<List<MarketItem>>,
        val marketMetrics: Result<MarketMetricsItem>,
        val topNftsCollections: Result<List<TopNftCollection>>,
        val topSectors: Result<List<Category>>,
    )

    private val disposables = CompositeDisposable()

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val viewItem = MutableLiveData<MarketOverviewModule.ViewItem>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()

    init {
        Observable
            .combineLatest(
                service.topGainersObservable,
                service.topLosersObservable,
                service.marketMetricsObservable,
                service.topNftCollectionsObservable,
                service.topSectorsObservable
            ) { t1, t2, t3, t4, t5 ->
                OverviewItemsWrapper(t1, t2, t3, t4, t5)
            }
            .subscribeIO { overviewItems ->
                val error = listOfNotNull(
                    overviewItems.topGainers.exceptionOrNull(),
                    overviewItems.topLosers.exceptionOrNull(),
                    overviewItems.marketMetrics.exceptionOrNull(),
                    overviewItems.topNftsCollections.exceptionOrNull(),
                    overviewItems.topSectors.exceptionOrNull(),
                ).firstOrNull()

                if (error != null) {
                    viewStateLiveData.postValue(ViewState.Error(error))
                } else {
                    val topGainerMarketItems = overviewItems.topGainers.getOrNull()
                    val topLoserMarketItems = overviewItems.topLosers.getOrNull()
                    val marketMetrics = overviewItems.marketMetrics.getOrNull()
                    val topNftCollections = overviewItems.topNftsCollections.getOrNull()
                    val topSectors = overviewItems.topSectors.getOrNull()

                    if (
                        topGainerMarketItems != null
                        && topLoserMarketItems != null
                        && marketMetrics != null
                        && topNftCollections != null
                        && topSectors != null
                    ) {
                        val viewItem = getViewItem(
                            topGainerMarketItems,
                            topLoserMarketItems,
                            marketMetrics,
                            topNftCollections,
                            topSectors,
                        )
                        this.viewItem.postValue(viewItem)
                        viewStateLiveData.postValue(ViewState.Success)
                    }
                }
            }.let {
                disposables.add(it)
            }

        service.start()
    }

    private fun getViewItem(
        topGainerMarketItems: List<MarketItem>,
        topLoserMarketItems: List<MarketItem>,
        marketMetricsItem: MarketMetricsItem,
        topNftCollectionsItems: List<TopNftCollection>,
        topSectors: List<Category>
    ): MarketOverviewModule.ViewItem {
        val topGainersBoard = getBoard(ListType.TopGainers, topGainerMarketItems)
        val topLosersBoard = getBoard(ListType.TopLosers, topLoserMarketItems)

        val boardItems = listOf(topGainersBoard, topLosersBoard)
        val marketMetrics = getMarketMetrics(marketMetricsItem)
        val topNftsBoard = topNftCollectionsBoard(topNftCollectionsItems)
        val topSectorsBoard = topSectorsBoard(topSectors)

        return MarketOverviewModule.ViewItem(marketMetrics, boardItems, topNftsBoard, topSectorsBoard)
    }

    private fun topSectorsBoard(items: List<Category>) =
        TopSectorsBoard(
            title = R.string.Market_Overview_TopSectors,
            iconRes = R.drawable.ic_categories_20,
            items = items
        )

    private fun topNftCollectionsBoard(items: List<TopNftCollection>) =
        TopNftCollectionsBoard(
            title = R.string.Nft_TopCollections,
            iconRes = R.drawable.ic_circle_up_20,
            timeDurationSelect = Select(service.topNftsTimeDuration, service.topNftsTimeDurationOptions),
            collections = items.mapIndexed { index, collection -> topNftCollectionViewItem(collection, index) }
        )

    private fun topNftCollectionViewItem(
        collection: TopNftCollection,
        index: Int
    ): TopNftCollectionViewItem {
        val volume: BigDecimal
        val volumeDiff: BigDecimal
        when (service.topNftsTimeDuration) {
            TimeDuration.OneDay -> {
                volume = collection.oneDayVolume
                volumeDiff = collection.oneDayVolumeDiff
            }
            TimeDuration.SevenDay -> {
                volume = collection.sevenDaysVolume
                volumeDiff = collection.sevenDaysVolumeDiff
            }
            TimeDuration.ThirtyDay -> {
                volume = collection.thirtyDaysVolume
                volumeDiff = collection.thirtyDaysVolumeDiff
            }
        }
        val volumeFormatted = numberFormatter.formatCoin(volume, "ETH", 0, 2)
        val floorPriceFormatted = collection.floorPrice?.let {
            numberFormatter.format(it, 0, 2, prefix = "Floor: ", suffix = " ETH")
        }

        return TopNftCollectionViewItem(
            uid = collection.uid,
            name = collection.name,
            imageUrl = collection.imageUrl,
            volume = volumeFormatted,
            volumeDiff = volumeDiff * BigDecimal.valueOf(100),
            order = index + 1,
            floorPrice = floorPriceFormatted
        )
    }

    private fun getBoard(type: ListType, marketItems: List<MarketItem>): Board {
        val topMarket = when (type) {
            ListType.TopGainers -> service.gainersTopMarket
            ListType.TopLosers -> service.losersTopMarket
        }
        val topList = marketItems.map { MarketViewItem.create(it, type.marketField) }

        val boardHeader = BoardHeader(
            getSectionTitle(type),
            getSectionIcon(type),
            Select(topMarket, service.topMarketOptions)
        )
        return Board(boardHeader, topList, type)
    }

    private fun getMarketMetrics(marketMetricsItem: MarketMetricsItem): MarketMetrics {
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
        val points = marketMetricsPoints.map { ChartPoint(it.value.toFloat(), it.timestamp) }
        return ChartDataBuilder.buildFromPoints(points)
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val (shortenValue, suffix) = App.numberFormatter.shortenValue(value)
        return App.numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + " $suffix"
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

    private fun refreshWithMinLoadingSpinnerPeriod() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    fun onSelectTopMarket(topMarket: TopMarket, listType: ListType) {
        when (listType) {
            ListType.TopGainers -> service.setGainersTopMarket(topMarket)
            ListType.TopLosers -> service.setLosersTopMarket(topMarket)
        }
    }

    fun onSelectTopNftsTimeDuration(timeDuration: TimeDuration) {
        service.setTopNftsTimeDuration(timeDuration)
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun getTopCoinsParams(listType: ListType): Triple<SortingField, TopMarket, MarketField> {
        return when (listType) {
            ListType.TopGainers -> {
                Triple(SortingField.TopGainers, service.gainersTopMarket, MarketField.PriceDiff)
            }
            ListType.TopLosers -> {
                Triple(SortingField.TopLosers, service.losersTopMarket, MarketField.PriceDiff)
            }
        }
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }
}
