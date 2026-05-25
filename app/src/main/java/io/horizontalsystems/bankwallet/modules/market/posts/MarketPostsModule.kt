package io.horizontalsystems.bankwallet.modules.market.posts

object MarketPostsModule {

    data class PostViewItem(
        val source: String,
        val title: String,
        val body: String,
        val timeAgo: String,
        val url: String
    )

}
