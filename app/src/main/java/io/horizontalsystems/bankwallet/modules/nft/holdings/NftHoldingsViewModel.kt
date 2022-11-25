package io.horizontalsystems.bankwallet.modules.nft.holdings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.ITotalBalance
import io.horizontalsystems.bankwallet.modules.balance.TotalBalance
import io.horizontalsystems.bankwallet.modules.balance.TotalService
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NftHoldingsViewModel(
    private val service: NftHoldingsService,
    private val totalBalance: TotalBalance
) : ViewModel(), ITotalBalance by totalBalance {

    val priceType by service::priceType

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var refreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    var viewItems by mutableStateOf<List<NftCollectionViewItem>>(listOf())
        private set

    init {
        viewModelScope.launch {
            service.itemsFlow.collect { sync(it) }
        }

        viewModelScope.launch {
            service.start()
        }

        totalBalance.start(viewModelScope)
    }

    private fun sync(items: List<NftHoldingsService.Item>) {
        viewState = ViewState.Success

        val expandedStates = viewItems.associate { it.uid to it.expanded }
        viewItems = items.map { viewItem(it, expandedStates[it.uid] ?: false) }

        totalBalance.setTotalServiceItems(
            items.map { it.assetItems }.flatten().mapNotNull { asset ->
                asset.price?.let { price ->
                    TotalService.BalanceItem(
                        price.value,
                        isValuePending = false,
                        coinPrice = asset.coinPrice
                    )
                }
            }
        )
    }

    private fun viewItem(item: NftHoldingsService.Item, expanded: Boolean) =
        NftCollectionViewItem(
            uid = item.uid,
            name = item.name,
            imageUrl = item.imageUrl,
            count = item.count,
            expanded = expanded,
            assets = item.assetItems.map { assetViewItem(it, item) }
        )

    private fun assetViewItem(assetItem: NftHoldingsService.AssetItem, item: NftHoldingsService.Item) =
        NftAssetViewItem(
            collectionUid = item.providerCollectionUid,
            nftUid = assetItem.nftUid,
            name = assetItem.name,
            imageUrl = assetItem.imageUrl,
            onSale = assetItem.onSale,
            count = assetItem.count,
            price = assetItem.price?.coinValue,
            priceInFiat = assetItem.priceInFiat
        )

    fun refresh() {
        viewModelScope.launch {
            refreshing = true
            service.refresh()
            delay(1000)
            refreshing = false
        }
    }

    fun updatePriceType(priceType: PriceType) {
        service.updatePriceType(priceType)
    }

    fun toggleCollection(collection: NftCollectionViewItem) {
        val index = viewItems.indexOf(collection)

        if (index != -1) {
            viewItems = viewItems.toMutableList().apply {
                this[index] = collection.copy(expanded = !collection.expanded)
            }
        }
    }

    fun errorShown() {
        errorMessage = null
    }

    override fun onCleared() {
        service.stop()
        totalBalance.stop()
    }

}