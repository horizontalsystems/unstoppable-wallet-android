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
import io.horizontalsystems.bankwallet.core.icon24
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.NftCollectionMetadata
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionModule.Tab
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NftCollectionOverviewViewModel(
    private val service: NftCollectionOverviewService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private var result: Result<NftCollectionMetadata>? = null

    val tabs = Tab.values()

    var overviewViewItem by mutableStateOf<NftCollectionOverviewViewItem?>(null)
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    val collectionUid: String
        get() = service.providerCollectionUid

    init {
        service.nftCollection.collectWith(viewModelScope) { result ->
            this.result = result
            sync()
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

    private fun sync() {
        overviewViewItem = result?.getOrNull()?.let { collection ->

            NftCollectionOverviewViewItem(
                name = collection.name,
                imageUrl = collection.imageUrl,
                description = collection.description,
                ownersCount = collection.ownerCount?.let { shortenValue(it.toBigDecimal()) } ?: "",
                totalSupply = shortenValue(collection.totalSupply.toBigDecimal()),
                links = links(collection),
                contracts = contracts(collection)
            )
        }

        viewState = result?.exceptionOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
    }

    private fun shortenValue(value: BigDecimal): String {
        return numberFormatter.formatNumberShort(value, 0)
    }

    private fun contracts(collection: NftCollectionMetadata) =
        collection.contracts.map {
            ContractInfo(
                it,
                service.blockchainType.icon24,
                service.blockchain?.explorerUrl?.replace("\$ref", it)
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

data class NftCollectionOverviewViewItem(
    val name: String,
    val imageUrl: String?,
    val description: String?,
    val ownersCount: String,
    val totalSupply: String,
    val links: List<Link>,
    val contracts: List<ContractInfo>
) {
    data class Link(
        val url: String,
        val title: TranslatableString,
        @DrawableRes val icon: Int
    )
}
