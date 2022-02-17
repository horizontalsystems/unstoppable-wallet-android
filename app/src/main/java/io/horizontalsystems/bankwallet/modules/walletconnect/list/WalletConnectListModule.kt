package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v1.WalletConnectListService
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v1.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v2.WC2ListService
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v2.WC2ListViewModel

object WalletConnectListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WalletConnectListService(App.wc1SessionManager)

            return WalletConnectListViewModel(service) as T
        }
    }

    class FactoryWC2 : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WC2ListService(App.wc2SessionManager)

            return WC2ListViewModel(service) as T
        }
    }

    data class Section(
        val version: Version,
        val sessions: List<SessionViewItem>,
        val pendingRequests: Int? = null
    )

    data class SessionViewItem(
        val sessionId: String,
        val title: String,
        val subtitle: String,
        val url: String,
        val imageUrl: String?,
    )

    enum class Version(val value: Int) {
        Version1(R.string.WalletConnect_Version1),
        Version2(R.string.WalletConnect_Version2)
    }

    enum class Chain(val value: Int) {
        Ethereum(1),
        BinanceSmartChain(56),
        Ropsten(3),
        Rinkeby(4),
        Kovan(42),
        Goerli(5),
    }

    fun getVersionFromUri(scannedText: String): Int {
        return when {
            scannedText.contains("@1?") -> 1
            scannedText.contains("@2?") -> 2
            else -> 0
        }
    }
}
