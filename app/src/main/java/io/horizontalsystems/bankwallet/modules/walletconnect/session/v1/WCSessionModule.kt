package io.horizontalsystems.bankwallet.modules.walletconnect.session.v1

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Request
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service

object WCSessionModule {

    class Factory(private val service: WC1Service) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCSessionViewModel(service, App.accountManager) as T
        }
    }

    fun prepareParams(remotePeerId: String?, connectionLink: String?) = bundleOf(
        REMOTE_PEER_ID_KEY to remotePeerId,
        CONNECTION_LINK_KEY to connectionLink
    )

    const val REMOTE_PEER_ID_KEY = "remote_peer_id"
    const val CONNECTION_LINK_KEY = "connection_link"

    data class PeerMetaItem(
        val name: String,
        val url: String,
        val description: String?,
        val icon: String?,
        val accountName: String?,
    )

    data class WCRequestWrapper(
        val wC1Request: WC1Request,
        val dAppName: String?
    )

}
