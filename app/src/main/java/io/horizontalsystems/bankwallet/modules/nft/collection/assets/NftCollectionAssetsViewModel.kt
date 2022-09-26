package io.horizontalsystems.bankwallet.modules.nft.collection.assets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.bankwallet.modules.nft.holdings.NftAssetViewItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NftCollectionAssetsViewModel(
    private val service: NftCollectionAssetsService
) : ViewModel() {

    var assets by mutableStateOf<List<NftAssetViewItem>?>(null)
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var loadingMore by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    init {
        service.items.collectWith(viewModelScope) { result ->
            result.getOrNull()?.let { list ->
                assets = list.map { viewItem(it) }
                loadingMore = false
            }

            viewState = result.exceptionOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
        }

        viewModelScope.launch {
            service.start()
        }
    }

    private fun viewItem(item: NftCollectionAssetsService.Item) =
        NftAssetViewItem(
            collectionUid = item.asset.providerCollectionUid,
            nftUid = item.asset.nftUid,
            name = item.asset.displayName,
            imageUrl = item.asset.previewImageUrl,
            count = 1,
            onSale = item.asset.saleInfo != null,
            price = item.price?.coinValue,
            priceInFiat = item.priceInFiat
        )

    fun onBottomReached() {
        loadingMore = !isRefreshing

        viewModelScope.launch {
            service.loadMore()
        }
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun refresh() {
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

}
