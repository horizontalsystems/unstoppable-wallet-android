package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.market.MarketModule.ListType
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.Board
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardHeader
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetrics
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsPoint
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopNftCollectionsBoard
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopPlatformsBoard
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopSectorsBoard
import io.horizontalsystems.bankwallet.modules.market.overview.TopSectorsRepository.Companion.getCategoryMarketData
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsViewItemFactory
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformItem
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformViewItem
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionItem
import io.horizontalsystems.bankwallet.modules.nft.nftCollectionItem
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MarketOverviewViewModel(
    private val service: MarketOverviewService,
    private val topNftCollectionsViewItemFactory: TopNftCollectionsViewItemFactory,
    private val currencyManager: CurrencyManager
) : ViewModel() {

    private val disposables = CompositeDisposable()

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
    var topPlatformsTimeDuration: TimeDuration = TimeDuration.OneDay
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
        Observable
            .combineLatest(
                service.topMoversObservable,
                service.marketOverviewObservable
            ) { t1, t2 ->
                Pair(t1, t2)
            }
            .subscribeIO { overviewItems ->
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
            }.let {
                disposables.add(it)
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

        val topGainersBoard = getBoard(ListType.TopGainers, topMovers)
        val topLosersBoard = getBoard(ListType.TopLosers, topMovers)

        return MarketOverviewModule.ViewItem(
            getMarketMetrics(marketOverview.globalMarketPoints, baseCurrency),
            listOf(topGainersBoard, topLosersBoard),
            topNftCollectionsBoard(nftCollectionItems),
            topSectorsBoard(coinCategoryItems),
            topPlatformsBoard(topPlatformItems)
        )
    }

    private fun topSectorsBoard(items: List<Category>) =
        TopSectorsBoard(
            title = R.string.Market_Overview_TopSectors,
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
                    subtitle = Translator.getString(
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

    private fun getBoard(type: ListType, topMovers: TopMovers): Board {
        val topMarket: TopMarket

        val marketInfoList = when (type) {
            ListType.TopGainers -> {
                topMarket = gainersTopMarket

                when (gainersTopMarket) {
                    TopMarket.Top100 -> topMovers.gainers100
                    TopMarket.Top200 -> topMovers.gainers200
                    TopMarket.Top300 -> topMovers.gainers300
                }
            }

            ListType.TopLosers -> {
                topMarket = losersTopMarket

                when (losersTopMarket) {
                    TopMarket.Top100 -> topMovers.losers100
                    TopMarket.Top200 -> topMovers.losers200
                    TopMarket.Top300 -> topMovers.losers300
                }
            }
        }

        val marketItems = marketInfoList.map { MarketItem.createFromCoinMarket(it, baseCurrency) }
        val topList = marketItems.map { MarketViewItem.create(it, type.marketField) }

        val boardHeader = BoardHeader(
            getSectionTitle(type),
            getSectionIcon(type),
            Select(topMarket, service.topMarketOptions)
        )
        return Board(boardHeader, topList, type)
    }

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
                MetricsType.DefiCap
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
        disposables.clear()
    }
}

val NftPrice.coinValue: CoinValue
    get() = CoinValue(token, value)
