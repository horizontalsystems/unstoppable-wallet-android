package cash.p.terminal.modules.market.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.CoinValue
import io.horizontalsystems.core.entities.Currency
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketModule.ListType
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.modules.market.overview.MarketOverviewModule.MarketMetrics
import cash.p.terminal.modules.market.overview.MarketOverviewModule.MarketMetricsPoint
import cash.p.terminal.modules.market.overview.MarketOverviewModule.TopNftCollectionsBoard
import cash.p.terminal.modules.market.overview.MarketOverviewModule.TopPlatformsBoard
import cash.p.terminal.modules.market.overview.MarketOverviewModule.TopSectorsBoard
import cash.p.terminal.modules.market.overview.TopSectorsRepository.Companion.getCategoryMarketData
import cash.p.terminal.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import cash.p.terminal.modules.market.topnftcollections.TopNftCollectionsViewItemFactory
import cash.p.terminal.modules.market.topplatforms.TopPlatformItem
import cash.p.terminal.modules.market.topplatforms.TopPlatformViewItem
import cash.p.terminal.modules.market.topplatforms.TopPlatformsRepository
import cash.p.terminal.modules.metricchart.MetricsType
import cash.p.terminal.modules.nft.NftCollectionItem
import cash.p.terminal.modules.nft.nftCollectionItem
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.extensions.MetricData
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.models.ChartPoint
import cash.p.terminal.wallet.models.GlobalMarketPoint
import io.horizontalsystems.core.models.HsTimePeriod
import cash.p.terminal.wallet.models.MarketOverview
import cash.p.terminal.wallet.models.NftPrice
import cash.p.terminal.wallet.models.TopMovers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class MarketOverviewViewModel(
    private val service: MarketOverviewService,
    private val topNftCollectionsViewItemFactory: TopNftCollectionsViewItemFactory,
    private val currencyManager: CurrencyManager
) : ViewModel() {

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val viewItem = MutableLiveData<MarketOverviewModule.ViewItem>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()

    val topNftCollectionsParams: Pair<SortingField, TimeDuration>
        get() = Pair(topNftsSortingField, topNftsTimeDuration)

    var gainersTopMarket: TopMarket = TopMarket.Top100
        private set
    var losersTopMarket: TopMarket = TopMarket.Top100
        private set
    var topNftsTimeDuration: TimeDuration = TimeDuration.SevenDay
        private set
    val topNftsSortingField: SortingField = SortingField.HighestVolume
    var topPlatformsTimeDuration: TimeDuration = TimeDuration.SevenDay
        private set

    var topMovers: TopMovers? = null
    var marketOverview: MarketOverview? = null

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    private fun syncViewItems() {
        val topMovers = topMovers ?: return
        val marketOverview = marketOverview ?: return

        val viewItem = getViewItem(
            topMovers,
            marketOverview
        )
        this.viewItem.postValue(viewItem)
        viewStateLiveData.postValue(ViewState.Success)
    }


    init {
        viewModelScope.launch {
            val overviewItemsFlow = service.topMoversObservable.asFlow()
                .combine(service.marketOverviewObservable.asFlow()) { t1, t2 ->
                    Pair(t1, t2)
                }

            overviewItemsFlow.collect { overviewItems ->
                val error = listOfNotNull(
                    overviewItems.first.exceptionOrNull(),
                    overviewItems.second.exceptionOrNull(),
                ).firstOrNull()

                if (error != null) {
                    viewStateLiveData.postValue(ViewState.Error(error))
                } else {
                    topMovers = overviewItems.first.getOrNull()
                    marketOverview = overviewItems.second.getOrNull()

                    if (
                        topMovers != null
                        && marketOverview != null
                    ) {
                        syncViewItems()
                    }
                }
            }
        }

        service.start()
    }

    private fun getViewItem(
        topMovers: TopMovers,
        marketOverview: MarketOverview
    ): MarketOverviewModule.ViewItem {
        val topPlatformItems = TopPlatformsRepository.getTopPlatformItems(marketOverview.topPlatforms, topPlatformsTimeDuration)
        val coinCategoryItems = marketOverview.coinCategories.map { category ->
            Category(category, getCategoryMarketData(category, baseCurrency))
        }

        val timePeriod = when(topNftsTimeDuration) {
            TimeDuration.OneDay -> HsTimePeriod.Day1
            TimeDuration.SevenDay -> HsTimePeriod.Week1
            TimeDuration.ThirtyDay -> HsTimePeriod.Month1
        }
        val nftCollectionItems = marketOverview.nftCollections.getOrElse(timePeriod) { listOf() }.map { it.nftCollectionItem }

//        val topGainersBoard = getBoard(ListType.TopGainers, topMovers)
//        val topLosersBoard = getBoard(ListType.TopLosers, topMovers)

        return MarketOverviewModule.ViewItem(
            marketMetrics = getMarketMetrics(marketOverview.globalMarketPoints, baseCurrency),
//            boards = listOf(topGainersBoard, topLosersBoard),
            topNftCollectionsBoard = topNftCollectionsBoard(nftCollectionItems),
            topSectorsBoard = topSectorsBoard(coinCategoryItems),
            topPlatformsBoard = topPlatformsBoard(topPlatformItems),
            topMarketPairs = marketOverview.topPairs.map {
                TopPairViewItem.createFromTopPair(it, baseCurrency.symbol)
            },
        )
    }

    private fun topSectorsBoard(items: List<Category>) =
        TopSectorsBoard(
            title = R.string.Market_Overview_Sectors,
            iconRes = R.drawable.ic_categories_20,
            items = items
        )

    private fun topNftCollectionsBoard(items: List<NftCollectionItem>) =
        TopNftCollectionsBoard(
            title = R.string.Nft_TopCollections,
            iconRes = R.drawable.ic_top_nft_collections_20,
            timeDurationSelect = Select(topNftsTimeDuration, service.timeDurationOptions),
            collections = items.mapIndexed { index, collection ->
                topNftCollectionsViewItemFactory.viewItem(collection, topNftsTimeDuration, index + 1)
            }
        )

    private fun topPlatformsBoard(items: List<TopPlatformItem>) =
        TopPlatformsBoard(
            title = R.string.MarketTopPlatforms_Title,
            iconRes = R.drawable.ic_blocks_20,
            timeDurationSelect = Select(
                topPlatformsTimeDuration,
                service.timeDurationOptions
            ),
            items = items.map { item ->
                TopPlatformViewItem(
                    platform = item.platform,
                    subtitle = cash.p.terminal.strings.helpers.Translator.getString(
                        R.string.MarketTopPlatforms_Protocols,
                        item.protocols
                    ),
                    marketCap = formatFiatShortened(item.marketCap, baseCurrency.symbol),
                    marketCapDiff = item.changeDiff,
                    rank = item.rank.toString(),
                    rankDiff = item.rankDiff,
                )
            }
        )

//    private fun getBoard(type: ListType, topMovers: TopMovers): Board {
//        val topMarket: TopMarket
//
//        val marketInfoList = when (type) {
//            ListType.TopGainers -> {
//                topMarket = gainersTopMarket
//
//                when (gainersTopMarket) {
//                    TopMarket.Top100 -> topMovers.gainers100
//                    TopMarket.Top200 -> topMovers.gainers200
//                    TopMarket.Top300 -> topMovers.gainers300
//                }
//            }
//
//            ListType.TopLosers -> {
//                topMarket = losersTopMarket
//
//                when (losersTopMarket) {
//                    TopMarket.Top100 -> topMovers.losers100
//                    TopMarket.Top200 -> topMovers.losers200
//                    TopMarket.Top300 -> topMovers.losers300
//                }
//            }
//        }
//
//        val marketItems = marketInfoList.map { MarketItem.createFromCoinMarket(it, baseCurrency) }
//        val topList = marketItems.map { MarketViewItem.create(it, type.marketField) }
//
//        val boardHeader = BoardHeader(
//            getSectionTitle(type),
//            getSectionIcon(type),
//            Select(topMarket, service.topMarketOptions)
//        )
//        return Board(boardHeader, topList, type)
//    }

    private fun getMarketMetrics(globalMarketPoints: List<GlobalMarketPoint>, baseCurrency: Currency): MarketMetrics {
        var marketCap: BigDecimal? = null
        var marketCapDiff: BigDecimal? = null
        var defiMarketCap: BigDecimal? = null
        var defiMarketCapDiff: BigDecimal? = null
        var volume24h: BigDecimal? = null
        var volume24hDiff: BigDecimal? = null
        var tvl: BigDecimal? = null
        var tvlDiff: BigDecimal? = null

        if (globalMarketPoints.isNotEmpty()) {
            val startingPoint = globalMarketPoints.first()
            val endingPoint = globalMarketPoints.last()

            marketCap = endingPoint.marketCap
            marketCapDiff = diff(startingPoint.marketCap, marketCap)

            defiMarketCap = endingPoint.defiMarketCap
            defiMarketCapDiff = diff(startingPoint.defiMarketCap, defiMarketCap)

            volume24h = endingPoint.volume24h
            volume24hDiff = diff(startingPoint.volume24h, volume24h)

            tvl = endingPoint.tvl
            tvlDiff = diff(startingPoint.tvl, tvl)
        }

        val totalMarketCapPoints = globalMarketPoints.map { MarketMetricsPoint(it.marketCap, it.timestamp) }
        val volume24Points = globalMarketPoints.map { MarketMetricsPoint(it.volume24h, it.timestamp) }
        val defiMarketCapPoints = globalMarketPoints.map { MarketMetricsPoint(it.defiMarketCap, it.timestamp) }
        val defiTvlPoints = globalMarketPoints.map { MarketMetricsPoint(it.tvl, it.timestamp) }

        return MarketMetrics(
            totalMarketCap = MetricData(
                marketCap?.let { formatFiatShortened(it, baseCurrency.symbol) },
                marketCapDiff,
                getChartData(totalMarketCapPoints),
                MetricsType.TotalMarketCap
            ),
            volume24h = MetricData(
                volume24h?.let { formatFiatShortened(it, baseCurrency.symbol) },
                volume24hDiff,
                getChartData(volume24Points),
                MetricsType.Volume24h
            ),
            defiCap = MetricData(
                defiMarketCap?.let { formatFiatShortened(it, baseCurrency.symbol) },
                defiMarketCapDiff,
                getChartData(defiMarketCapPoints),
                MetricsType.Etf
            ),
            defiTvl = MetricData(
                tvl?.let { formatFiatShortened(it, baseCurrency.symbol) },
                tvlDiff,
                getChartData(defiTvlPoints),
                MetricsType.TvlInDefi
            )
        )
    }

    private fun getChartData(marketMetricsPoints: List<MarketMetricsPoint>): ChartData? {
        if (marketMetricsPoints.isEmpty()) return null

        val points = marketMetricsPoints.map { ChartPoint(it.value.toFloat(), it.timestamp) }
        return ChartData(points, true, false)
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        return App.numberFormatter.formatFiatShort(value, symbol, 2)
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

    private fun diff(sourceValue: BigDecimal, targetValue: BigDecimal): BigDecimal =
        if (sourceValue.compareTo(BigDecimal.ZERO) != 0)
            ((targetValue - sourceValue) * BigDecimal(100)) / sourceValue
        else BigDecimal.ZERO

    fun onSelectTopMarket(topMarket: TopMarket, listType: ListType) {
        when (listType) {
            ListType.TopGainers -> {
                gainersTopMarket = topMarket
                syncViewItems()
            }
            ListType.TopLosers -> {
                losersTopMarket = topMarket
                syncViewItems()
            }
        }
    }

    fun onSelectTopNftsTimeDuration(timeDuration: TimeDuration) {
        topNftsTimeDuration = timeDuration
        syncViewItems()
    }

    fun onSelectTopPlatformsTimeDuration(timeDuration: TimeDuration) {
        topPlatformsTimeDuration = timeDuration
        syncViewItems()
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
                Triple(SortingField.TopGainers, gainersTopMarket, MarketField.PriceDiff)
            }
            ListType.TopLosers -> {
                Triple(SortingField.TopLosers, losersTopMarket, MarketField.PriceDiff)
            }
        }
    }

    override fun onCleared() {
        service.stop()
    }
}

val NftPrice.coinValue: CoinValue
    get() = CoinValue(token, value)
