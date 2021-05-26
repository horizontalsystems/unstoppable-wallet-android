package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_link.*

class CoinLinksAdapter(
        linksLiveData: MutableLiveData<List<CoinLink>>,
        viewLifecycleOwner: LifecycleOwner,
        private val listener: Listener
) : ListAdapter<CoinLink, CoinLinksAdapter.ViewHolder>(diff) {

    init {
        linksLiveData.observe(viewLifecycleOwner) {
            submitList(it)
        }
    }

    interface Listener {
        fun onClick(coinLink: CoinLink)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflate(parent, R.layout.view_holder_coin_link, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<CoinLink>() {
            override fun areItemsTheSame(oldItem: CoinLink, newItem: CoinLink): Boolean = true

            override fun areContentsTheSame(oldItem: CoinLink, newItem: CoinLink): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ViewHolder(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: CoinLink) {
            linkView.showTitle(containerView.context.getString(item.title))
            linkView.showIcon(ContextCompat.getDrawable(containerView.context, item.icon))
            linkView.setOnClickListener { listener.onClick(item) }
            item.listPosition?.let { linkView.setListPosition(it) }
        }
    }
}
