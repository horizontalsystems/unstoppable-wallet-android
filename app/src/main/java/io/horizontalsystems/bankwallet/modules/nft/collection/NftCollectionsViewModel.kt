package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nft.NftAsset
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NftCollectionsViewModel(private val service: NftCollectionsService) : ViewModel() {
    var priceType by mutableStateOf(PriceType.Days7)
        private set

    var viewState by mutableStateOf<ViewState?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    var collections by mutableStateOf<List<ViewItemNftCollection>>(listOf())
        private set

    init {
        viewModelScope.launch {
            service.nftCollections
                .collect {
                    handleNftCollections(it)
                }
        }

        service.start()
    }

    private fun handleNftCollections(nftCollectionsState: DataState<Pair<List<NftCollection>, List<NftAsset>>>) {
        loading = nftCollectionsState.loading

        nftCollectionsState.dataOrNull?.let { (collections, assets) ->
            viewState = ViewState.Success

            syncItems(collections, assets)
        }
    }

    private fun syncItems(collections: List<NftCollection>, assets: List<NftAsset>) {
        val expandedStates = this.collections.map {
            it.slug to it.expanded
        }.toMap()

        val assetsGrouped = assets.groupBy { it.collectionSlug }

        this.collections = collections.map { collection ->
            val collectionAssets = assetsGrouped[collection.slug] ?: listOf()
            ViewItemNftCollection(
                slug = collection.slug,
                name = collection.name,
                imageUrl = collection.imageUrl,
                ownedAssetCount = collectionAssets.size,
                expanded = expandedStates[collection.slug] ?: false,
                assets = collectionAssets.map { asset ->
                    ViewItemNftAsset(
                        tokenId = asset.tokenId,
                        name = asset.name,
                        imagePreviewUrl = asset.imagePreviewUrl,
                        coinPrice = CoinValue(CoinValue.Kind.Coin(Coin("", "Ethereum", "ETH"), 8), BigDecimal(100.123)),
                        currencyPrice = CurrencyValue(Currency("USD", "$", 2), BigDecimal("112.2979871")),
                        onSale = true
                    )
                }
            )
        }
    }

    override fun onCleared() {
        service.stop()
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            service.refresh()
            loading = false
        }
    }

    fun changePriceType(priceType: PriceType) {
        this.priceType = priceType
    }

    fun toggleCollection(collection: ViewItemNftCollection) {
        val index = collections.indexOf(collection)

        if (index != -1) {
            collections = collections.toMutableList().apply {
                this[index] = collection.copy(expanded = !collection.expanded)
            }
        }
    }

}
