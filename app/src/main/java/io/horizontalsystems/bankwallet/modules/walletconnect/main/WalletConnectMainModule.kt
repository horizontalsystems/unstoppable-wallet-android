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

    fun start(fragment: Fragment, navigateTo: Int, navOptions: NavOptions, remotePeerId: String? = null) {
        fragment.findNavController().navigate(navigateTo, bundleOf(REMOTE_PEER_ID_KEY to remotePeerId), navOptions)
    }

    const val REMOTE_PEER_ID_KEY = "remote_peer_id"

}
