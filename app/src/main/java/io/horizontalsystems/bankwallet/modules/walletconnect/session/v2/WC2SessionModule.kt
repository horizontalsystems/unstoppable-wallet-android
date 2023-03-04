package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
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

    data class BlockchainViewItem(
        val chainId: Int,
        val name: String,
        val address: String,
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
