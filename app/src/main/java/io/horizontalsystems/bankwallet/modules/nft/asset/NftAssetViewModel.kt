package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.viewState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class NftAssetViewModel(private val service: NftAssetService) : ViewModel() {
    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var nftAssetItem by mutableStateOf<NftAssetModuleAssetItem?>(null)
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    init {
        service.serviceDataFlow
            .collectWith(viewModelScope) { result ->
                result.viewState?.let {
                    viewState = it
                }
                nftAssetItem = result.getOrNull()
                errorMessage = result.exceptionOrNull()?.let { errorText(it) }
            }

        viewModelScope.launch {
            service.start()
        }
    }

    override fun onCleared() {
        service.stop()
    }

    fun refresh() {
        viewModelScope.launch {
            service.refresh()
        }
    }

    private fun errorText(error: Throwable): TranslatableString {
        return when (error) {
            is UnknownHostException -> TranslatableString.ResString(R.string.Hud_Text_NoInternet)
            else -> TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
        }
    }

    fun errorShown() {
        errorMessage = null
    }
}
