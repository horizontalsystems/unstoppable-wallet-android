package io.horizontalsystems.bankwallet.modules.nft.collection.overview

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.eip20TokenUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.NftCollectionMetadata
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionModule.Tab
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftPrice
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NftCollectionOverviewViewModel(
    private val service: NftCollectionOverviewService,
    private val numberFormatter: IAppNumberFormatter,
    xRateService: XRateService,
    marketKit: MarketKitWrapper
) : ViewModel() {

    private val baseToken = marketKit.token(TokenQuery(service.blockchainType, TokenType.Native))
    private var result: Result<NftCollectionMetadata>? = null
    private var rate: CurrencyValue? = baseToken?.let { xRateService.getRate(baseToken.coin.uid) }

    val tabs = Tab.values()

    var overviewViewItem by mutableStateOf<NftCollectionOverviewViewItem?>(null)
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    val collectionUid: String
        get() = service.providerCollectionUid

    val blockchainType: BlockchainType
        get() = service.blockchainType

    var contracts: List<ContractInfo> = listOf()
        private set

    init {
        service.nftCollection.collectWith(viewModelScope) { result ->
            sync(result, rate)
        }

        baseToken?.let {
            xRateService.getRateFlow(baseToken.coin.uid).collectWith(viewModelScope) { rate ->
                result?.let {
                    sync(it, rate)
                }
            }
        }

        viewModelScope.launch {
            service.start()
        }
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

    private fun formatNftPrice(nftPrice: NftPrice) =
        numberFormatter.formatCoinShort(nftPrice.value, nftPrice.token.coin.code, nftPrice.token.decimals)

    private fun formatCurrencyValue(value: BigDecimal?, rate: CurrencyValue?): String? {
        if (value == null || rate == null) return null

        return numberFormatter.formatFiatShort(value * rate.value, rate.currency.symbol, 2)
    }

    @Synchronized
    private fun sync(result: Result<NftCollectionMetadata>, rate: CurrencyValue?) {
        overviewViewItem = result.getOrNull()?.let { collection ->
            contracts = contracts(collection)

            NftCollectionOverviewViewItem(
                name = collection.name,
                imageUrl = collection.imageUrl ?: collection.thumbnailImageUrl,
                description = collection.description,
                ownersCount = collection.ownerCount?.let { shortenValue(it.toBigDecimal()) } ?: "",
                totalSupply = collection.totalSupply?.toBigDecimal()?.let { shortenValue(it) } ?: "",
                floorPrice = collection.floorPrice?.let { PriceViewItem(formatNftPrice(it), formatCurrencyValue(it.value, rate)) },
                oneDayVolume = collection.stats1d?.volume?.let { formatNftPrice(it) },
                oneDayVolumeDiff = collection.stats1d?.change,
                oneDaySellersCount = collection.stats1d?.sales,
                oneDaySellersAveragePrice = collection.stats1d?.averagePrice?.let { "~${formatNftPrice(it)} per NFT" },
                royalty = collection.royalty?.let { numberFormatter.format(it, 0, 2, suffix = "%") },
                inceptionDate = collection.inceptionDate?.let { DateHelper.formatDate(it, "MMM dd, yyyy") },
                links = links(collection),
                contracts = contracts
            )
        }

        viewState = result.exceptionOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
    }

    private fun shortenValue(value: BigDecimal): String {
        return numberFormatter.formatNumberShort(value, 0)
    }

    private fun contracts(collection: NftCollectionMetadata) =
        collection.contracts.map {
            ContractInfo(
                it.address,
                service.blockchainType.imageUrl,
                service.blockchain?.eip20TokenUrl(it.address),
                it.name,
                it.schemaName
            )
        }

    private fun links(collection: NftCollectionMetadata) = buildList {
        collection.externalUrl?.let {
            add(
                NftCollectionOverviewViewItem.Link(
                    url = it,
                    title = TranslatableString.ResString(R.string.NftAsset_Links_Website),
                    icon = R.drawable.ic_globe_20
                )
            )
        }
        collection.providerUrl?.let {
            add(
                NftCollectionOverviewViewItem.Link(
                    url = collection.providerUrl,
                    title = TranslatableString.PlainString(service.providerTitle),
                    icon = service.providerIcon
                )
            )
        }
        collection.discordUrl?.let {
            add(
                NftCollectionOverviewViewItem.Link(
                    url = it,
                    title = TranslatableString.ResString(R.string.NftAsset_Links_Discord),
                    icon = R.drawable.ic_discord_20
                )
            )
        }
        collection.twitterUsername?.let {
            add(
                NftCollectionOverviewViewItem.Link(
                    url = "https://twitter.com/$it",
                    title = TranslatableString.ResString(R.string.NftAsset_Links_Twitter),
                    icon = R.drawable.ic_twitter_20
                )
            )
        }
    }

}

data class PriceViewItem(val coinValue: String, val fiatValue: String?)

data class NftCollectionOverviewViewItem(
    val name: String,
    val imageUrl: String?,
    val description: String?,
    val ownersCount: String,
    val totalSupply: String,
    val floorPrice: PriceViewItem?,
    val oneDayVolume: String?,
    val oneDayVolumeDiff: BigDecimal?,
    val oneDaySellersCount: Int?,
    val oneDaySellersAveragePrice: String?,
    val royalty: String?,
    val inceptionDate: String?,
    val links: List<Link>,
    val contracts: List<ContractInfo>
) {
    data class Link(
        val url: String,
        val title: TranslatableString,
        @DrawableRes val icon: Int
    )
}
