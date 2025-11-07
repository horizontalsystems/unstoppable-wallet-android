package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCRequestViewItem

object WalletConnectListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return WalletConnectListViewModel(
                App.wcSessionManager,
                App.wcManager,
            ) as T
        }
    }

    data class SessionViewItem(
        val sessionTopic: String,
        val title: String,
        val subtitle: String,
        val url: String,
        val imageUrl: String?,
        val pendingRequestsCount: Int = 0,
        val requests: List<WCRequestViewItem>,
    )

    fun getVersionFromUri(scannedText: String): Int {
        return when {
            scannedText.contains("@2") -> 2
            else -> 0
        }
    }

}
