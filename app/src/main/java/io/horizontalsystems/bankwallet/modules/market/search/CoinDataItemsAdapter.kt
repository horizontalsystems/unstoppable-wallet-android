package io.horizontalsystems.bankwallet.modules.market.search

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_item.*

class CoinDataItemsAdapter(private val onItemClick: (CoinDataViewItem) -> Unit) : ListAdapter<CoinDataViewItem, CoinDataItemsAdapter.ViewHolder>(diffCallback) {

    class ViewHolder(override val containerView: View, onItemClick: (CoinDataViewItem) -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private var item: CoinDataViewItem? = null

        init {
            rank.isVisible = false

            itemView.setOnSingleClickListener {
                item?.let {
                    onItemClick(it)
                }
            }
        }

        fun bind(item: CoinDataViewItem) {
            this.item = item

            val drawableResId = AppLayoutHelper.getCoinDrawableResId(containerView.context, item.type)
                    ?: R.drawable.coin_placeholder
            icon.setImageResource(drawableResId)

            title.text = item.name
            subtitle.text = item.code
        }

        companion object {
            fun create(parent: ViewGroup, viewType: Int, onItemClick: (CoinDataViewItem) -> Unit) = ViewHolder(inflate(parent, R.layout.view_holder_market_item), onItemClick)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder.create(parent, viewType, onItemClick)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<CoinDataViewItem>() {
            override fun areItemsTheSame(oldItem: CoinDataViewItem, newItem: CoinDataViewItem) = false
            override fun areContentsTheSame(oldItem: CoinDataViewItem, newItem: CoinDataViewItem) = false
        }
    }
}
