package io.horizontalsystems.bankwallet.modules.nft

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class NftsViewModel(private val service: NftsService) : ViewModel() {
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

    private fun handleNftCollections(nftCollectionsState: DataState<List<NftCollection>>) {
        loading = nftCollectionsState.loading

        nftCollectionsState.dataOrNull?.let {
            viewState = ViewState.Success

            syncItems(it)
        }
    }

    private fun syncItems(collections: List<NftCollection>) {
        this.collections = collections.map {
            ViewItemNftCollection(
                slug = it.slug,
                name = it.name,
                imageUrl = it.imageUrl,
                ownedAssetCount = 1,
                expanded = false,
                listOf()
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
