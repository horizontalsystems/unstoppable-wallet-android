package cash.p.terminal.modules.walletconnect.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.modules.walletconnect.list.v1.WalletConnectListService
import cash.p.terminal.modules.walletconnect.list.v1.WalletConnectListViewModel

object WalletConnectListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WalletConnectListService(App.wc1SessionManager, App.evmBlockchainManager)

            return WalletConnectListViewModel(
                service,
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
        Version1(R.string.WalletConnect_Version1),
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