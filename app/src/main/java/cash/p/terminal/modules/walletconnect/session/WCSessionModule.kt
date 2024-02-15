package cash.p.terminal.modules.walletconnect.session
>>>>>>>> 3a48e845b (Refactor WalletConnect, use Web3Wallet API):app/src/main/java/cash/p/terminal/modules/walletconnect/session/WC2SessionModule.kt

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.walletconnect.web3.wallet.client.Wallet
import cash.p.terminal.R
import cash.p.terminal.core.App
import kotlinx.parcelize.Parcelize
>>>>>>>> 3a48e845b (Refactor WalletConnect, use Web3Wallet API):app/src/main/java/cash/p/terminal/modules/walletconnect/session/WC2SessionModule.kt

object WCSessionModule {

    class Factory(
        private val sessionTopic: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCSessionViewModel(
                App.wcSessionManager,
                App.connectivityManager,
                App.accountManager.activeAccount,
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

data class WCRequestViewItem(
    val requestId: Long,
    val title: String,
    val subtitle: String,
    val request: Wallet.Model.SessionRequest
)

data class WCSessionUiState(
    val peerMeta: PeerMetaItem?,
    val closeEnabled: Boolean,
    val connecting: Boolean,
    val buttonStates: WCSessionButtonStates?,
    val hint: Int?,
    val showError: String?,
    val status: Status?,
    val pendingRequests: List<WCRequestViewItem>
)

enum class Status(val value: Int) {
    OFFLINE(R.string.WalletConnect_Status_Offline),
    ONLINE(R.string.WalletConnect_Status_Online),
    CONNECTING(R.string.WalletConnect_Status_Connecting)
}

data class PeerMetaItem(
    val name: String,
    val url: String,
    val description: String?,
    val icon: String?,
    val accountName: String?,
)

sealed class WCSessionServiceState {
    object Idle : WCSessionServiceState()
    class Invalid(val error: Throwable) : WCSessionServiceState()
    object WaitingForApproveSession : WCSessionServiceState()
    object Ready : WCSessionServiceState()
    object Killed : WCSessionServiceState()
}