package io.horizontalsystems.bankwallet.modules.market.search

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.label
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer

class CoinDataItemsAdapter(private val onItemClick: (CoinDataViewItem) -> Unit) : ListAdapter<CoinDataViewItem, CoinDataItemsAdapter.ViewHolder>(diffCallback) {

    class ViewHolder(override val containerView: View, onItemClick: (CoinDataViewItem) -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private var item: CoinDataViewItem? = null
        private val icon = containerView.findViewById<ImageView>(R.id.icon)
        private val title = containerView.findViewById<TextView>(R.id.title)
        private val subtitle = containerView.findViewById<TextView>(R.id.subtitle)
        private val rank = containerView.findViewById<TextView>(R.id.rank)

        init {
            itemView.setOnSingleClickListener {
                item?.let {
                    onItemClick(it)
                }
            }
        }

        fun bind(item: CoinDataViewItem) {
            this.item = item

            val drawableResId = AppLayoutHelper.getCoinDrawableResId(containerView.context, item.type)
                    ?: R.drawable.place_holder
            icon.setImageResource(drawableResId)

            title.text = item.name
            subtitle.text = item.code

            if(item.type is CoinType.Erc20 || item.type is CoinType.Bep20  || item.type is CoinType.Bep2) {
                rank.text = item.type.label
                rank.isVisible = true
            }
            else
                rank.isVisible = false
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
