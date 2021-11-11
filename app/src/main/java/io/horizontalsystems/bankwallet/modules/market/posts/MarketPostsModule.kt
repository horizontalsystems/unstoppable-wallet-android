package io.horizontalsystems.bankwallet.modules.market.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarketPostsModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val postsService = MarketPostService(App.marketKit, App.backgroundManager)
            return MarketPostsViewModel(postsService) as T
        }

    }

    data class PostViewItem(
        val source: String,
        val title: String,
        val body: String,
        val timeAgo: String,
        val url: String
    )

}
