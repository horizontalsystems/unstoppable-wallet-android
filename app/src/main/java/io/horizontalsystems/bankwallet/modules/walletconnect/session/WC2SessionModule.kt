package io.horizontalsystems.bankwallet.modules.walletconnect.session

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import kotlinx.parcelize.Parcelize

object WC2SessionModule {

    class Factory(
        private val sessionTopic: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WC2SessionViewModel(
                App.wc2SessionManager,
                App.connectivityManager,
                App.accountManager.activeAccount,
                sessionTopic,
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

data class WC2RequestViewItem(
    val requestId: Long,
    val title: String,
    val subtitle: String,
    val request: Wallet.Model.SessionRequest
)

data class WC2SessionUiState(
    val peerMeta: PeerMetaItem?,
    val closeEnabled: Boolean,
    val connecting: Boolean,
    val buttonStates: WCSessionButtonStates?,
    val hint: Int?,
    val showError: String?,
    val status: Status?,
    val pendingRequests: List<WC2RequestViewItem>
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

sealed class WC2SessionServiceState {
    object Idle : WC2SessionServiceState()
    class Invalid(val error: Throwable) : WC2SessionServiceState()
    object WaitingForApproveSession : WC2SessionServiceState()
    object Ready : WC2SessionServiceState()
    object Killed : WC2SessionServiceState()
}