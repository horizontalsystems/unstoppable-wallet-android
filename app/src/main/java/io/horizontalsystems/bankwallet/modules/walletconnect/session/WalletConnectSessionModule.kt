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

    fun prepareParams(remotePeerId: String?, sessionsCount: Int) = bundleOf(
        REMOTE_PEER_ID_KEY to remotePeerId,
        SESSIONS_COUNT_KEY to sessionsCount
    )

    const val REMOTE_PEER_ID_KEY = "remote_peer_id"
    const val SESSIONS_COUNT_KEY = "sessions_count"

}
