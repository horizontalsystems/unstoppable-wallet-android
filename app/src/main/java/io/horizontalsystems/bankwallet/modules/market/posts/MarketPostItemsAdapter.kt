package io.horizontalsystems.bankwallet.modules.market.posts

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.modules.market.posts.MarketPostsModule.PostViewItem

class MarketPostItemsAdapter(
    private val listener: ViewHolderMarketPostItem.Listener,
    itemsLiveData: LiveData<List<PostViewItem>>,
    viewLifecycleOwner: LifecycleOwner
) : ListAdapter<PostViewItem, ViewHolderMarketPostItem>(coinRateDiff) {

    init {
        itemsLiveData.observe(viewLifecycleOwner, {
            submitList(it)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketPostItem {
        return ViewHolderMarketPostItem.create(parent, listener)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketPostItem, position: Int, payloads: MutableList<Any>) {
        holder.bind(
                getItem(position),
                payloads.firstOrNull { it is PostViewItem } as? PostViewItem,
        )
    }

    override fun onBindViewHolder(holder: ViewHolderMarketPostItem, position: Int) = Unit

    companion object {
        private val coinRateDiff = object : DiffUtil.ItemCallback<PostViewItem>() {
            override fun areItemsTheSame(oldItem: PostViewItem, newItem: PostViewItem): Boolean {
                return oldItem.title == newItem.title && oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: PostViewItem, newItem: PostViewItem): Boolean {
                return oldItem.body == newItem.body
            }

            override fun getChangePayload(oldItem: PostViewItem, newItem: PostViewItem): Any? {
                return oldItem
            }
        }
    }
}
