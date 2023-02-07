package cash.p.terminal.modules.releasenotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object ReleaseNotesModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReleaseNotesViewModel(
                App.networkManager,
                App.releaseNotesManager.releaseNotesUrl,
                App.connectivityManager,
                App.appConfigProvider
            ) as T
        }
    }
}
