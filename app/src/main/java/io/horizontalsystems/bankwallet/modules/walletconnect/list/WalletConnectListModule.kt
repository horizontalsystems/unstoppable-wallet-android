package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.core.findNavController

object WalletConnectListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = WalletConnectListService(App.predefinedAccountTypeManager, App.walletConnectSessionManager)

            return WalletConnectListViewModel(service, StringProvider()) as T
        }
    }

    fun start(fragment: Fragment, navigateTo: Int, navOptions: NavOptions) {
        fragment.findNavController().navigate(navigateTo, null, navOptions)
    }
}
