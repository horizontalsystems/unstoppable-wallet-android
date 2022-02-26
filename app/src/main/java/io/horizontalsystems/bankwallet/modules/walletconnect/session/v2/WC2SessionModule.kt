package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService

object WC2SessionModule {

    class Factory(
        private val sessionTopic: String?,
        private val connectionLink: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val sessionService = WC2SessionService(
                App.wc2Service,
                App.wc2Manager,
                App.wc2SessionManager,
                App.accountManager,
                WC2PingService(),
                App.connectivityManager,
                sessionTopic,
                connectionLink,
            )

            return WC2SessionViewModel(sessionService) as T
        }
    }

    fun prepareParams(sessionTopic: String?, connectionLink: String?) = bundleOf(
        SESSION_TOPIC_KEY to sessionTopic,
        CONNECTION_LINK_KEY to connectionLink,
    )

    const val SESSION_TOPIC_KEY = "session_topic_id"
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
