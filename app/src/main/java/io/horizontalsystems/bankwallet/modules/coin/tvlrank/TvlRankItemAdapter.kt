package io.horizontalsystems.bankwallet.modules.coin.tvlrank

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setCoinImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_item.*
import java.math.BigDecimal

class TvlRankItemAdapter : ListAdapter<TvlRankViewItem, ViewHolderTvlRank>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderTvlRank {
        return ViewHolderTvlRank.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolderTvlRank, position: Int) = Unit

    override fun onBindViewHolder(
        holder: ViewHolderTvlRank,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(
            getItem(position),
            payloads.firstOrNull { it is TvlRankViewItem } as? TvlRankViewItem)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<TvlRankViewItem>() {
            override fun areItemsTheSame(
                oldItem: TvlRankViewItem,
                newItem: TvlRankViewItem
            ): Boolean {
                return oldItem.data == newItem.data
            }

            override fun areContentsTheSame(
                oldItem: TvlRankViewItem,
                newItem: TvlRankViewItem
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(
                oldItem: TvlRankViewItem,
                нewItem: TvlRankViewItem
            ): Any? {
                return oldItem
            }
        }
    }
}

class ViewHolderTvlRank(override val containerView: View) : RecyclerView.ViewHolder(containerView),
    LayoutContainer {

    fun bind(item: TvlRankViewItem, prev: TvlRankViewItem?) {
        if (item.data.type != prev?.data?.type) {
            icon.setCoinImage(item.data.type)
        }

        if (item.data.title != prev?.data?.title) {
            title.text = item.data.title
        }

        if (item.tvlRank != prev?.tvlRank) {
            rank.text = item.tvlRank
        }

        if (item.chains != prev?.chains) {
            subtitle.text = item.chains
        }

        if (item.tvl != prev?.tvl) {
            rate.text = item.tvl
        }

        if (item.tvlDiff != prev?.tvlDiff) {
            val value = item.tvlDiff
            val sign = if (value >= BigDecimal.ZERO) "+" else "-"
            marketFieldValue.text = App.numberFormatter.format(value.abs(), 0, 2, sign, "%")

            val color = if (value >= BigDecimal.ZERO) R.color.remus else R.color.lucian
            marketFieldValue.setTextColor(containerView.context.getColor(color))
        }
    }

    companion object {
        fun create(parent: ViewGroup): ViewHolderTvlRank {
            return ViewHolderTvlRank(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_holder_market_item, parent, false)
            )
        }
    }
}
