package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nft.collection.NftAssetItem
import kotlinx.coroutines.launch

class NftAssetViewModel(private val service: NftAssetService) : ViewModel() {
    var viewState by mutableStateOf<ViewState?>(null)
        private set

    var assetItem by mutableStateOf<NftAssetItem?>(null)
        private set

    init {
        viewModelScope.launch {
            assetItem = service.fetchAsset()
            viewState = ViewState.Success
        }
    }
}
