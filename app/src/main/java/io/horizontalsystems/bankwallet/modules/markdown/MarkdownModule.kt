package io.horizontalsystems.bankwallet.modules.markdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarkdownModule {

    class Factory(private val guideUrl: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarkdownViewModel(guideUrl, App.connectivityManager, App.networkManager) as T
        }
    }
}
