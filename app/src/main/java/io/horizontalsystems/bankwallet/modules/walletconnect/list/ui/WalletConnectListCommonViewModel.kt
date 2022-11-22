package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service

class WalletConnectListCommonViewModel(private val wc2Service: WC2Service) : ViewModel() {

    sealed class Page {
        data class WC1Session(val uri: String) : Page()
        object Error: Page()
    }

    var page by mutableStateOf<Page?>(null)
        private set

    fun setUri(uri: String) {
        page = when (getVersionFromUri(uri)) {
            1 -> Page.WC1Session(uri)
            2 -> {
                wc2Service.pair(uri)
                null
            }
            else -> Page.Error
        }
    }

    fun setConnectionUri(uri: String): Page? {
        return when (getVersionFromUri(uri)) {
            1 -> Page.WC1Session(uri)
            2 -> {
                wc2Service.pair(uri)
                null
            }
            else -> Page.Error
        }
    }

    private fun getVersionFromUri(uri: String) = WalletConnectListModule.getVersionFromUri(uri)

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletConnectListCommonViewModel(App.wc2Service) as T
        }
    }
}
