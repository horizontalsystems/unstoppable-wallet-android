package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexProviderManager
import io.horizontalsystems.bankwallet.modules.balance.cex.CexAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DepositAddressViewModel(
    private val cexAsset: CexAsset,
    private val networkId: String?,
    private val cexProviderManager: CexProviderManager
) : ViewModel() {
    private val cexProvider = cexProviderManager.cexProviderFlow.value

    private var address: CexAddress? = null
    private var error: Throwable? = null
    private var loading = false

    var uiState by mutableStateOf(
        DepositAddress(
            loading = loading,
            address = address,
            error = error
        )
    )

    init {
        loading = true
        emitState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                address = cexProvider?.getAddress(cexAsset.id, networkId)
            } catch (t: Throwable) {
                error = t
            }
            loading = false
            emitState()
        }
    }

    private fun emitState() {
        uiState = DepositAddress(
            loading = loading,
            address = address,
            error = error
        )
    }

    class Factory(private val cexAsset: CexAsset, private val networkId: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DepositAddressViewModel(cexAsset, networkId, App.cexProviderManager) as T
        }
    }
}

data class DepositAddress(val loading: Boolean, val address: CexAddress?, val error: Throwable?)