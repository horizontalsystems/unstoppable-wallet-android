package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2RequestViewItem
import io.horizontalsystems.ethereumkit.models.Chain

object WC2SessionModule {

    class Factory(
        private val sessionTopic: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WC2SessionViewModel(
                App.wc2Service,
                App.wc2Manager,
                App.wc2SessionManager,
                App.accountManager,
                App.connectivityManager,
                App.evmBlockchainManager,
                sessionTopic,
            ) as T
        }
    }

    fun prepareParams(sessionTopic: String?) = bundleOf(
        SESSION_TOPIC_KEY to sessionTopic
    )

    const val SESSION_TOPIC_KEY = "session_topic_id"
}

enum class WCButtonState(val visible: Boolean, val enabled: Boolean) {
    Enabled(true, true),
    Disabled(true, false),
    Hidden(false, true)
}

data class WCSessionButtonStates(
    val connect: WCButtonState,
    val disconnect: WCButtonState,
    val cancel: WCButtonState,
    val remove: WCButtonState,
)

data class WCBlockchain(
    val chainId: Int,
    val name: String,
    val address: String,
) {
    val chainNamespace = "eip155"

    override fun equals(other: Any?): Boolean {
        return other is WCBlockchain && this.chainId == other.chainId
    }

    override fun hashCode(): Int {
        return chainId.hashCode()
    }

    fun getAccount() = "$chainNamespace:$chainId:$address"
}

data class WCAccountData(
    val eip: String,
    val chain: Chain,
    val address: String?
)

data class WC2SessionUiState(
    val peerMeta: PeerMetaItem?,
    val closeEnabled: Boolean,
    val connecting: Boolean,
    val buttonStates: WCSessionButtonStates?,
    val hint: Int?,
    val showError: String?,
    val blockchains: List<BlockchainViewItem>,
    val status: Status?,
    val pendingRequests: List<WC2RequestViewItem>
)

enum class Status(val value: Int) {
    OFFLINE(R.string.WalletConnect_Status_Offline),
    ONLINE(R.string.WalletConnect_Status_Online),
    CONNECTING(R.string.WalletConnect_Status_Connecting)
}

data class BlockchainViewItem(
    val chainId: Int,
    val name: String,
    val address: String,
)

data class PeerMetaItem(
    val name: String,
    val url: String,
    val description: String?,
    val icon: String?,
    val accountName: String?,
)
