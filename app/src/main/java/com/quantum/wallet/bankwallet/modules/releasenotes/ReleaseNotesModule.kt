package com.quantum.wallet.bankwallet.modules.releasenotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App

object ReleaseNotesModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReleaseNotesViewModel(
                App.networkManager,
                App.releaseNotesManager.releaseNotesUrl,
                App.connectivityManager,
                App.releaseNotesManager,
                App.appConfigProvider
            ) as T
        }
    }
}
