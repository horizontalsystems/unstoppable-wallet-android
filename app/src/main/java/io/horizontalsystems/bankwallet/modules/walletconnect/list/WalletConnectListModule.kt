package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object WalletConnectListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return WalletConnectListViewModel(
                App.wc2SessionManager,
                App.evmBlockchainManager,
                App.wc2Service
            ) as T
        }
    }

    data class Section(
        val version: Version,
        val sessions: List<SessionViewItem>,
    )

    data class SessionViewItem(
        val sessionId: String,
        val title: String,
        val subtitle: String,
        val url: String,
        val imageUrl: String?,
        val pendingRequestsCount: Int = 0,
    )

    enum class Version(val value: Int) {
        Version2(R.string.WalletConnect_Version2)
    }

    fun getVersionFromUri(scannedText: String): Int {
        return when {
            scannedText.contains("@1") -> 1
            scannedText.contains("@2") -> 2
            else -> 0
        }
    }

}
