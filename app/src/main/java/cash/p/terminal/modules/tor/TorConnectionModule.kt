package cash.p.terminal.modules.tor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object TorConnectionModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TorConnectionViewModel(App.torKitManager, App.connectivityManager) as T
        }
    }

    data class TorViewState(
        val stateText: Int,
        val showRetryButton: Boolean,
        val torIsActive: Boolean,
        val showNetworkConnectionError: Boolean,
    )

}
