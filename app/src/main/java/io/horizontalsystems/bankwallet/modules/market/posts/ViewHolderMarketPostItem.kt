package io.horizontalsystems.bankwallet.modules.market.posts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_post.*

class ViewHolderMarketPostItem(override val containerView: View, private val listener: Listener)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var item: MarketOverviewModule.PostViewItem? = null

    interface Listener {
        fun onPostClick(postViewItem: MarketOverviewModule.PostViewItem)
    }

    init {
        containerView.setOnClickListener {
            item?.let {
                listener.onPostClick(it)
            }
        }
    }

    fun bind(item: MarketOverviewModule.PostViewItem, prev: MarketOverviewModule.PostViewItem?) {
        this.item = item

        if (item.title != prev?.title) {
            postTitle.text = item.title
        }

        if (item.source != prev?.source) {
            postSource.text = item.source
        }

        if (item.body != prev?.body) {
            postBody.text = item.body
        }

        if (item.timeAgo != prev?.timeAgo) {
            postTime.text = item.timeAgo
        }

    }

    companion object {
        fun create(parent: ViewGroup, listener: Listener): ViewHolderMarketPostItem {
            return ViewHolderMarketPostItem(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_post, parent, false), listener)
        }
    }
}
