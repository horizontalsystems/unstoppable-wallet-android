package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.WalletConnectSession

object WalletConnectListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WalletConnectListService(App.wc1SessionManager)

            return WalletConnectListViewModel(service) as T
        }
    }

    data class Section(
        val version: Version,
        val sessions: List<Session>,
        val pendingRequests: Int? = null
    )

    data class Session(
        val session: WalletConnectSession,
        val title: String,
        val subtitle: String,
        val url: String,
        val imageUrl: String?,
    )

    enum class Version(val value: Int) {
        Version1(R.string.WalletConnect_Version1),
        Version2(R.string.WalletConnect_Version2)
    }
}
