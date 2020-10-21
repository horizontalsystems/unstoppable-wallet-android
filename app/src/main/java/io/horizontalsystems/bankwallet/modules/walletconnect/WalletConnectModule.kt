package io.horizontalsystems.bankwallet.modules.walletconnect

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object WalletConnectModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = WalletConnectService(App.ethereumKitManager, App.walletConnectSessionStore)

            return WalletConnectViewModel(service, listOf(service)) as T
        }
    }

}
