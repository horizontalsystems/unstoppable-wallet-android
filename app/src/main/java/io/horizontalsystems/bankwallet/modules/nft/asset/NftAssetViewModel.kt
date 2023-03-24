package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata.SaleType
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata.Trait
import io.horizontalsystems.bankwallet.entities.nft.NftCollectionMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.viewState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import kotlin.math.roundToInt

class NftAssetViewModel(private val service: NftAssetService) : ViewModel() {
    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var viewItem by mutableStateOf<ViewItem?>(null)
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    val nftUid by service::nftUid

    val tabs = NftAssetModule.Tab.values()

    init {
        service.serviceDataFlow
            .collectWith(viewModelScope) { result ->
                result.viewState?.let {
                    viewState = it
                }
                viewItem = result.getOrNull()?.let { viewItem(it) }
                errorMessage = result.exceptionOrNull()?.let { errorText(it) }
            }

        viewModelScope.launch {
            service.start()
        }
    }

    private fun viewItem(item: NftAssetService.Item): ViewItem {
        val asset = item.asset
        val collection = item.collection

        return ViewItem(
            nftUid = asset.nftUid,
            imageUrl = asset.imageUrl,
            name = asset.displayName,
            providerCollectionUid = asset.providerCollectionUid,
            collectionName = collection.name,
            lastSale = priceViewItem(item.lastSale),
            average7d = priceViewItem(item.average7d),
            average30d = priceViewItem(item.average30d),
            collectionFloor = priceViewItem(item.collectionFloor),
            bestOffer = priceViewItem(item.bestOffer),
            sale = saleViewItem(item.sale),
            traits = asset.traits.map { traitViewItem(it, collection.totalSupply) },
            description = asset.description,
            contractAddress = asset.nftUid.contractAddress,
            schemaName = asset.nftType,
            links = linkViewItems(asset, collection),
            showSend = item.owned,
        )
    }

    private fun linkViewItems(asset: NftAssetMetadata, collection: NftCollectionMetadata): List<LinkViewItem> {
        val viewItems = mutableListOf<LinkViewItem>()
        asset.externalLink?.let {
            viewItems.add(LinkViewItem(LinkType.Website, it))
        }
        asset.providerLink?.let {
            viewItems.add(LinkViewItem(LinkType.Provider(service.providerTitle, service.providerIcon), it))
        }
        collection.discordUrl?.let {
            viewItems.add(LinkViewItem(LinkType.Discord, it))
        }
        collection.twitterUsername?.let {
            viewItems.add(LinkViewItem(LinkType.Twitter, "https://twitter.com/$it"))
        }
        return viewItems
    }

    private fun traitViewItem(trait: Trait, totalSupply: Int?) =
        TraitViewItem(
            type = trait.type,
            value = trait.value,
            percent = totalSupply?.let { getAttributePercentage(trait, it) },
            searchUrl = trait.searchUrl
        )

    private fun getAttributePercentage(trait: Trait, totalSupply: Int): String? =
        if (trait.count > 0 && totalSupply > 0) {
            val percent = (trait.count * 100f / totalSupply)
            val number = when {
                percent >= 10 -> percent.roundToInt()
                percent >= 1 -> (percent * 10).roundToInt() / 10f
                else -> (percent * 100).roundToInt() / 100f
            }
            "$number%"
        } else {
            null
        }

    private fun saleViewItem(sale: NftAssetService.SaleItem?) =
        sale?.let {
            sale.bestListing?.let { listing ->
                SaleViewItem(
                    untilDate = TranslatableString.ResString(R.string.Nfts_Asset_OnSaleUntil, DateHelper.getFullDate(listing.untilDate)),
                    type = sale.type,
                    price = PriceViewItem(coinValue(listing.price), fiatValue(listing.price))
                )
            }
        }

    private fun priceViewItem(priceItem: NftAssetService.PriceItem?): PriceViewItem? =
        priceItem?.let {
            PriceViewItem(coinValue(it), fiatValue(it))
        }

    private fun fiatValue(priceItem: NftAssetService.PriceItem): String =
        priceItem.priceInFiat?.getFormattedFull() ?: "---"

    private fun coinValue(priceItem: NftAssetService.PriceItem) =
        priceItem.price?.let {
            CoinValue(it.token, it.value).getFormattedFull()
        } ?: "---"

    fun refresh() {
        viewModelScope.launch {
            service.refresh()
        }
    }

    private fun errorText(error: Throwable): TranslatableString =
        when (error) {
            is UnknownHostException -> TranslatableString.ResString(R.string.Hud_Text_NoInternet)
            else -> TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
        }

    fun errorShown() {
        errorMessage = null
    }

    data class ViewItem(
        val nftUid: NftUid,
        val imageUrl: String?,
        val name: String,
        val providerCollectionUid: String,
        val collectionName: String,
        val lastSale: PriceViewItem?,
        val average7d: PriceViewItem?,
        val average30d: PriceViewItem?,
        val collectionFloor: PriceViewItem?,
        val bestOffer: PriceViewItem?,
        val sale: SaleViewItem?,
        val traits: List<TraitViewItem>,
        val description: String?,
        val contractAddress: String,
        val schemaName: String?,
        val links: List<LinkViewItem>,
        val showSend: Boolean,
    ) {

        val providerUrl: Pair<String, String>?
            get() = (links.firstOrNull { it.type is LinkType.Provider })?.let { linkViewItem ->
                (linkViewItem.type as? LinkType.Provider)?.let {
                    Pair(it.title, linkViewItem.url)
                }
            }
    }

    data class SaleViewItem(
        val untilDate: TranslatableString,
        val type: SaleType,
        val price: PriceViewItem,
    )

    data class PriceViewItem(val coinValue: String, val fiatValue: String)

    data class TraitViewItem(
        val type: String,
        val value: String,
        val percent: String?,
        val searchUrl: String?
    )

    data class LinkViewItem(val type: LinkType, val url: String)

    sealed class LinkType {
        class Provider(val title: String, val icon: Int) : LinkType()
        object Website : LinkType()
        object Discord : LinkType()
        object Twitter : LinkType()
    }
}
