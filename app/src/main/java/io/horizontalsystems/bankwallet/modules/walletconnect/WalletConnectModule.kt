package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController

object WalletConnectModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = WalletConnectService(App.ethereumKitManager, App.walletConnectSessionManager, App.connectivityManager)

            return WalletConnectViewModel(service, listOf(service)) as T
        }
    }

    fun start(fragment: Fragment, navigateTo: Int, navOptions: NavOptions) {
        if (App.ethereumKitManager.evmKit != null) {
            fragment.findNavController().navigate(navigateTo, null, navOptions)
        } else {
            ConfirmationDialog.show(
                    icon = R.drawable.ic_wallet_connect_24,
                    title = fragment.getString(R.string.WalletConnect_Title),
                    subtitle = fragment.getString(R.string.WalletConnect_Requirement),
                    contentText = fragment.getString(R.string.WalletConnect_RequirementDescription),
                    actionButtonTitle = fragment.getString(R.string.Button_Add),
                    cancelButtonTitle = null,
                    activity = fragment.requireActivity(),
                    listener = object : ConfirmationDialog.Listener {
                        override fun onActionButtonClick() {
                            fragment.findNavController().navigate(R.id.manageWalletsFragment, null, navOptions)
                        }
                    }
            )
        }
    }

}
