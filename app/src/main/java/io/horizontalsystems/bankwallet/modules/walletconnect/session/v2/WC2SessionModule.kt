package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service

object WC2SessionModule {

    class Factory(
        private val remotePeerId: String?,
        private val connectionLink: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WC2Service(
                remotePeerId,
                connectionLink,
                App.wc2SessionManager,
            )

            val sessionService = WC2SessionService(service, WC2PingService(), App.connectivityManager, null)

            return WC2SessionViewModel(sessionService) as T
        }
    }

    fun prepareParams(remotePeerId: String?, connectionLink: String?) = bundleOf(
        REMOTE_PEER_ID_KEY to remotePeerId,
        CONNECTION_LINK_KEY to connectionLink,
    )

    const val REMOTE_PEER_ID_KEY = "remote_peer_id"
    const val CONNECTION_LINK_KEY = "connection_link"

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
    val reconnect: WCButtonState
)
