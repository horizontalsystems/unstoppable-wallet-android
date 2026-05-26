package io.horizontalsystems.bankwallet.modules.walletconnect.session

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

object WCSessionModule {

    @Serializable
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

data class WCRequestViewItem(
    val title: String,
    val subtitle: String,
    val request: HSDAppRequest
)

enum class WCWhiteListState {
    NotInWhiteList,
    InWhiteList,
    Error,
    InProgress
}

data class WCSessionUiState(
    val peerMeta: PeerMetaItem?,
    val closeEnabled: Boolean,
    val connecting: Boolean,
    val connected: Boolean,
    val buttonStates: WCSessionButtonStates?,
    val hint: String?,
    val showError: String?,
    val status: Status?,
    val pendingRequests: List<WCRequestViewItem>,
    val blockchainTypes: List<BlockchainType>?,
    val whiteListState: WCWhiteListState?,
    val hasSubscription: Boolean,
    val scamProtectionActionAllowed: Boolean,
    val closeDialog: Boolean,
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