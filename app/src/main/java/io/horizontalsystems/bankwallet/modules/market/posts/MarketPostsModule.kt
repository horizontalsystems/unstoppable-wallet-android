package io.horizontalsystems.bankwallet.modules.market.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarketPostsModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val postsService = MarketPostService(App.xRateManager, App.backgroundManager)
            return MarketPostsViewModel(postsService, listOf(postsService)) as T
        }

    }

    data class PostViewItem(
        val timeAgo: String,
        val imageUrl: String?,
        val source: String,
        val title: String,
        val url: String,
        val body: String
    )

}
