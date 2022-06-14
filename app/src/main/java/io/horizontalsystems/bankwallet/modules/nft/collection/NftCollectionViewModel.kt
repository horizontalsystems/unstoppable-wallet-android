package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionModule.Tab
import io.horizontalsystems.bankwallet.modules.nft.oneDayAveragePriceChange
import io.horizontalsystems.bankwallet.modules.nft.oneDayFloorPriceChange
import io.horizontalsystems.bankwallet.modules.nft.oneDaySalesChange
import io.horizontalsystems.bankwallet.modules.nft.oneDayVolumeChange
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.NftCollection
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NftCollectionViewModel(
    private val service: NftCollectionService,
    private val numberFormatter: IAppNumberFormatter,
    xRateService: XRateService,
    coinManager: ICoinManager
) : ViewModel() {

    private val basePlatformCoin: PlatformCoin = coinManager.getPlatformCoin(CoinType.Ethereum)!!
    private var result: Result<NftCollection>? = null
    private var rate: CurrencyValue? = xRateService.getRate(basePlatformCoin.coin.uid)

    var selectedTabLiveData = MutableLiveData(Tab.Overview)
    val tabs = Tab.values()

    var overviewViewItem by mutableStateOf<NftCollectionOverviewViewItem?>(null)
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    val collectionUid: String
        get() = service.collectionUid

    init {
        service.nftCollection.collectWith(viewModelScope) { result ->
            this.result = result
            sync()
        }

        xRateService.getRateFlow(basePlatformCoin.coin.uid).collectWith(viewModelScope) { rate ->
            this.rate = rate
            syncRate()
        }

        viewModelScope.launch {
            service.start()
        }
    }

    fun onSelect(tab: Tab) {
        selectedTabLiveData.postValue(tab)
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            isRefreshing = true

            service.refresh()

            delay(1000)
            isRefreshing = false
        }
    }

    private fun coinValue(value: BigDecimal) =
        numberFormatter.formatCoinShort(value, basePlatformCoin.code, basePlatformCoin.decimals)

    private fun currencyValue(value: BigDecimal?, rate: CurrencyValue?): String? {
        if (value == null || rate == null) return null

        return App.numberFormatter.formatFiatShort(value * rate.value, rate.currency.symbol, 2)
    }

    private fun syncRate() {
        val collection = result?.getOrNull() ?: return
        val item = overviewViewItem ?: return

        overviewViewItem = item.copy(
            volumeChartDataWrapper = item.volumeChartDataWrapper?.copy(
                secondaryValue = currencyValue(collection.stats.volumes[HsTimePeriod.Day1]?.value, rate)
            ),
            averagePriceChartDataWrapper = item.averagePriceChartDataWrapper?.copy(
                secondaryValue = currencyValue(collection.stats.averagePrice1d?.value, rate)
            ),
            floorPriceChartDataWrapper = item.floorPriceChartDataWrapper?.copy(
                secondaryValue = currencyValue(collection.stats.floorPrice?.value, rate),
            )
        )
    }

    private fun sync() {
        overviewViewItem = result?.getOrNull()?.let { collection ->
            val oneDayVolume = collection.stats.volumes[HsTimePeriod.Day1]?.value
            val volumeChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.Market_Volume24h),
                chartPoints = collection.statCharts?.oneDayVolumePoints?.map { Pair(it.value.toFloat(), it.timestamp) } ?: listOf(),
                primaryValue = oneDayVolume?.let { coinValue(it) } ?: "",
                secondaryValue = currencyValue(oneDayVolume, rate),
                change = collection.oneDayVolumeChange
            )

            val averagePrice = collection.stats.averagePrice1d?.value
            val averagePriceChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.NftCollection_AveragePrice),
                chartPoints = collection.statCharts?.averagePricePoints?.map { Pair(it.value.toFloat(), it.timestamp) } ?: listOf(),
                primaryValue = averagePrice?.let { coinValue(it) } ?: "",
                secondaryValue = currencyValue(averagePrice, rate),
                change = collection.oneDayAveragePriceChange
            )

            val oneDaySales = collection.stats.sales[HsTimePeriod.Day1]
            val salesChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.NftCollection_TodaysSales),
                chartPoints = collection.statCharts?.oneDaySalesPoints?.map { Pair(it.value.toFloat(), it.timestamp) } ?: listOf(),
                primaryValue = oneDaySales?.let { Translator.getString(R.string.NftCollection_ItemsCount, it) } ?: "",
                secondaryValue = Translator.getString(
                    R.string.NftCollection_AveragePriceDescription, averagePrice?.let { coinValue(it) } ?: ""
                ),
                change = collection.oneDaySalesChange
            )

            val floorPrice = collection.stats.floorPrice
            val floorPriceChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.NftAsset_Price_Floor),
                chartPoints = collection.statCharts?.floorPricePoints?.map { Pair(it.value.toFloat(), it.timestamp) } ?: listOf(),
                primaryValue = floorPrice?.let { coinValue(it.value) } ?: Translator.getString(R.string.NotAvailable),
                secondaryValue = currencyValue(floorPrice?.value, rate),
                change = collection.oneDayFloorPriceChange
            )

            NftCollectionOverviewViewItem(
                name = collection.name,
                imageUrl = collection.imageUrl,
                description = collection.description,
                ownersCount = collection.stats.ownersCount?.let { shortenValue(it.toBigDecimal()) } ?: "",
                totalSupply = collection.stats.totalSupply?.let { shortenValue(it.toBigDecimal()) } ?: "",
                volumeChartDataWrapper = volumeChartDataWrapper,
                averagePriceChartDataWrapper = averagePriceChartDataWrapper,
                salesChartDataWrapper = salesChartDataWrapper,
                floorPriceChartDataWrapper = floorPriceChartDataWrapper,
                links = links(collection),
                contracts = contracts(collection)
            )
        }

        viewState = result?.exceptionOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
    }

    private fun shortenValue(value: BigDecimal): String {
        return numberFormatter.formatNumberShort(value, 0)
    }

    private fun contracts(collection: NftCollection) =
        collection.asset_contracts?.map {
            ContractInfo(
                it.address,
                R.drawable.logo_ethereum_24,
                "https://etherscan.io/token/${it.address}"
            )
        } ?: listOf()

    private fun links(collection: NftCollection) = buildList {
        collection.externalUrl?.let {
            add(
                NftCollectionOverviewViewItem.Link(
                    url = it,
                    title = R.string.NftAsset_Links_Website,
                    icon = R.drawable.ic_globe_20
                )
            )
        }
        add(
            NftCollectionOverviewViewItem.Link(
                url = "https://opensea.io/collection/${collection.uid}",
                title = R.string.NftAsset_Links_OpenSea,
                icon = R.drawable.ic_opensea_20
            )
        )
        collection.discordUrl?.let {
            add(
                NftCollectionOverviewViewItem.Link(
                    url = it,
                    title = R.string.NftAsset_Links_Discord,
                    icon = R.drawable.ic_discord_20
                )
            )
        }
        collection.twitterUsername?.let {
            add(
                NftCollectionOverviewViewItem.Link(
                    url = "https://twitter.com/$it",
                    title = R.string.NftAsset_Links_Twitter,
                    icon = R.drawable.ic_twitter_20
                )
            )
        }
    }

    private fun chartDataWrapper(
        title: String,
        chartPoints: List<Pair<Float, Long>>,
        primaryValue: String,
        secondaryValue: String?,
        change: BigDecimal?
    ): NftCollectionOverviewViewItem.ChartDataWrapper? {
        if (chartPoints.isEmpty()) return null

        val chartData = ChartDataBuilder.buildFromPoints(points = chartPoints.map { (value, timestamp) ->
            ChartPoint(value, timestamp)
        })

        return NftCollectionOverviewViewItem.ChartDataWrapper(
            title = title,
            chartData = chartData,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            diff = change?.let { Value.Percent(it.multiply(BigDecimal(100))) }
        )
    }

}

data class NftCollectionOverviewViewItem(
    val name: String,
    val imageUrl: String?,
    val description: String?,
    val ownersCount: String,
    val totalSupply: String,
    val volumeChartDataWrapper: ChartDataWrapper?,
    val averagePriceChartDataWrapper: ChartDataWrapper?,
    val salesChartDataWrapper: ChartDataWrapper?,
    val floorPriceChartDataWrapper: ChartDataWrapper?,
    val links: List<Link>,
    val contracts: List<ContractInfo>
) {
    data class ChartDataWrapper(
        val title: String,
        val chartData: ChartData,
        val primaryValue: String,
        val secondaryValue: String?,
        val diff: Value?
    )

    data class Link(
        val url: String,
        @StringRes val title: Int,
        @DrawableRes val icon: Int
    )
}
