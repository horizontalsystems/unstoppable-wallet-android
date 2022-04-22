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
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionModule.Tab
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.chartview.models.ChartPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NftCollectionViewModel(
    private val service: NftCollectionService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    var selectedTabLiveData = MutableLiveData(Tab.Overview)
    val tabs = Tab.values()

    var nftCollectionOverviewUiState by mutableStateOf(
        NftCollectionOverviewUiState(
            isLoading = true
        )
    )
        private set

    val viewState = mutableStateOf<ViewState>(ViewState.Loading)

    init {
        service.nftCollection.collectWith(viewModelScope) { result ->
            handleNftCollection(result)
        }

        viewModelScope.launch {
            service.start()
        }
    }

    private fun handleNftCollection(result: Result<NftCollection>) {
        val itemUiState = result.getOrNull()?.let { collection ->

            val volumeChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.Market_Volume24h),
                chartPoints = collection.chartPoints.map { Pair(it.oneDayVolume.toFloat(), it.timestamp) },
                primaryValue = numberFormatter.formatCoinValueAsShortened(collection.oneDayVolume, "ETH"),
                secondaryValue = "N/A", // TODO
                change = collection.oneDayVolumeChange
            )
            val averagePriceChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.NftCollection_AveragePrice),
                chartPoints = collection.chartPoints.map { Pair(it.averagePrice.toFloat(), it.timestamp) },
                primaryValue = numberFormatter.formatCoinValueAsShortened(collection.oneDayAveragePrice, "ETH"),
                secondaryValue = "N/A", // TODO
                change = BigDecimal.ZERO // TODO
            )
            val salesChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.NftCollection_TodaysSales),
                chartPoints = collection.chartPoints.map { Pair(it.oneDaySales.toFloat(), it.timestamp) },
                primaryValue = Translator.getString(R.string.NftCollection_ItemsCount, collection.oneDaySales),
                secondaryValue = "N/A", // TODO
                change = BigDecimal.ZERO // TODO
            )

            val floorPriceChartDataWrapper = chartDataWrapper(
                title = Translator.getString(R.string.NftAsset_Price_Floor),
                chartPoints = collection.chartPoints.mapNotNull {
                    it.floorPrice?.let { floorPrice -> Pair(floorPrice.toFloat(), it.timestamp) }
                },
                primaryValue = collection.floorPrice?.let { numberFormatter.formatCoinValueAsShortened(it, "ETH") }
                    ?: Translator.getString(R.string.NotAvailable),
                secondaryValue = "N/A", // TODO
                change = BigDecimal.ZERO // TODO
            )

            NftCollectionOverviewItemUiState(
                name = collection.name,
                imageUrl = collection.imageUrl,
                description = collection.description,
                ownersCount = shortenValue(collection.ownersCount.toBigDecimal()),
                totalSupply = shortenValue(collection.totalSupply.toBigDecimal()),
                volumeChartDataWrapper = volumeChartDataWrapper,
                averagePriceChartDataWrapper = averagePriceChartDataWrapper,
                salesChartDataWrapper = salesChartDataWrapper,
                floorPriceChartDataWrapper = floorPriceChartDataWrapper,
                links = links(collection),
                contracts = contracts(collection)
            )
        }

        val errorMessages = result.exceptionOrNull()?.let {
            listOf(it.localizedMessage ?: it.javaClass.simpleName)
        } ?: listOf()

        nftCollectionOverviewUiState = NftCollectionOverviewUiState(
            isLoading = false,
            isRefreshing = false,
            errorMessages = errorMessages,
            item = itemUiState
        )
    }

    private fun contracts(collection: NftCollection) =
        collection.contracts.map {
            ContractInfo(
                it.address,
                R.drawable.logo_ethereum_24,
                "https://etherscan.io/token/${it.address}"
            )
        }

    private fun links(collection: NftCollection) = buildList {
        collection.links?.externalUrl?.let {
            add(
                NftCollectionOverviewItemUiState.Link(
                    url = it,
                    title = R.string.NftAsset_Links_Website,
                    icon = R.drawable.ic_globe_20
                )
            )
        }
        add(
            NftCollectionOverviewItemUiState.Link(
                url = "https://opensea.io/collection/${collection.uid}",
                title = R.string.NftAsset_Links_OpenSea,
                icon = R.drawable.ic_opensea_20
            )
        )
        collection.links?.discordUrl?.let {
            add(
                NftCollectionOverviewItemUiState.Link(
                    url = it,
                    title = R.string.NftAsset_Links_Discord,
                    icon = R.drawable.ic_discord_20
                )
            )
        }
        collection.links?.twitterUsername?.let {
            add(
                NftCollectionOverviewItemUiState.Link(
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
        secondaryValue: String,
        change: BigDecimal
    ): NftCollectionOverviewItemUiState.ChartDataWrapper? {
        if (chartPoints.isEmpty()) return null

        val chartData = ChartDataBuilder.buildFromPoints(points = chartPoints.map { (value, timestamp) ->
            ChartPoint(value, timestamp)
        })
        return NftCollectionOverviewItemUiState.ChartDataWrapper(
            title = title,
            chartData = chartData,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            diff = Value.Percent(change.multiply(BigDecimal(100)))
        )
    }

    private fun shortenValue(value: BigDecimal) =
        numberFormatter.shortenValue(value).let { "${it.first} ${it.second}" }

    fun onSelect(tab: Tab) {
        selectedTabLiveData.postValue(tab)
    }

    fun refresh() {
        TODO("not implemented")
    }

    fun onErrorClick() {
        TODO("not implemented")
    }

}

data class NftCollectionOverviewUiState(
    val isLoading: Boolean,
    val isRefreshing: Boolean = false,
    val errorMessages: List<String> = listOf(),
    val item: NftCollectionOverviewItemUiState? = null
)

data class NftCollectionOverviewItemUiState(
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
        val secondaryValue: String,
        val diff: Value
    )
    data class Link(
        val url: String,
        @StringRes val title: Int,
        @DrawableRes val icon: Int
    )
}
