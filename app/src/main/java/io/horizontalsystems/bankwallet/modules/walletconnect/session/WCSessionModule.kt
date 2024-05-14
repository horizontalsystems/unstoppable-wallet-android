package io.horizontalsystems.bankwallet.modules.walletconnect.session

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import kotlinx.parcelize.Parcelize

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
                App.evmBlockchainManager
            ) as T
        }
    }

    @Parcelize
    data class Input(val sessionTopic: String) : Parcelable
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
    val hint: String?,
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