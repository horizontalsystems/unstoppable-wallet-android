package io.horizontalsystems.bankwallet.modules.markdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.Single

object MarkdownModule {

    class Factory(private val guideUrl: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val contentProvider = MarkdownContentProvider(App.networkManager)
            return MarkdownViewModel(guideUrl, App.connectivityManager, contentProvider) as T
        }
    }

    interface IMarkdownContentProvider{
        fun getContent(contentUrl: String): Single<String>
    }
}
