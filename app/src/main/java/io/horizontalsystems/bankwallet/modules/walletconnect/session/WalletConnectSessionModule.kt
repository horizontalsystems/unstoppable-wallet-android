package io.horizontalsystems.bankwallet.modules.walletconnect.session

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService

object WalletConnectSessionModule {

    class Factory(private val service: WalletConnectService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletConnectMainViewModel(service) as T
        }
    }

    fun prepareParams(remotePeerId: String?, connectionLink: String?) = bundleOf(
        REMOTE_PEER_ID_KEY to remotePeerId,
        CONNECTION_LINK_KEY to connectionLink
    )

    const val REMOTE_PEER_ID_KEY = "remote_peer_id"
    const val CONNECTION_LINK_KEY = "connection_link"

}
