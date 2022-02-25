package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.ViewState
import kotlinx.coroutines.launch

class NftAssetViewModel(private val service: NftAssetService) : ViewModel() {
    var viewState by mutableStateOf<ViewState?>(null)
        private set

    var nftAssetItem by mutableStateOf<NftAssetModuleAssetItem?>(null)
        private set

    init {
        viewModelScope.launch {
            nftAssetItem = service.fetchItem()
            viewState = ViewState.Success
        }
    }
}
