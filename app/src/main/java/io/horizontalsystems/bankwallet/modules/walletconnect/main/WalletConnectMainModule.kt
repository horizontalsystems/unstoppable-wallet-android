package io.horizontalsystems.bankwallet.modules.walletconnect.main

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.core.findNavController

object WalletConnectMainModule {

    class Factory(private val service: WalletConnectService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return WalletConnectMainViewModel(service) as T
        }
    }

    fun start(fragment: Fragment, navigateTo: Int, navOptions: NavOptions, sessionsCount: Int = 0, remotePeerId: String? = null) {
        val arguments = bundleOf(
                REMOTE_PEER_ID_KEY to remotePeerId,
                SESSIONS_COUNT_KEY to sessionsCount
        )
        fragment.findNavController().navigate(navigateTo, arguments, navOptions)
    }

    const val REMOTE_PEER_ID_KEY = "remote_peer_id"
    const val SESSIONS_COUNT_KEY = "sessions_count"

}
