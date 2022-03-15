package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nft.viewState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class NftAssetViewModel(private val service: NftAssetService) : ViewModel() {
    var viewState by mutableStateOf<ViewState?>(null)
        private set

    var nftAssetItem by mutableStateOf<NftAssetModuleAssetItem?>(null)
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    init {
        viewModelScope.launch {
            service.serviceDataFlow
                .collect { assetData ->
                    viewState = assetData.viewState
                    nftAssetItem = assetData.value
                    errorMessage = assetData.error?.let { errorText(it) }
                }
        }

        service.start()
    }

    fun refresh() {
        viewModelScope.launch {
            service.refresh()
        }
    }

    private fun errorText(error: Exception): TranslatableString {
        return when (error) {
            is UnknownHostException -> TranslatableString.ResString(R.string.Hud_Text_NoInternet)
            else -> TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
        }
    }
}
