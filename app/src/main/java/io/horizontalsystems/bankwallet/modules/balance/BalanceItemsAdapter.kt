package io.horizontalsystems.bankwallet.modules.balance

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate

class BalanceItemsAdapter(private val listener: Listener) : ListAdapter<BalanceViewItem, RecyclerView.ViewHolder>(BalanceViewItemDiff()) {

    interface Listener {
        fun onSendClicked(viewItem: BalanceViewItem)
        fun onReceiveClicked(viewItem: BalanceViewItem)
        fun onSwapClicked(viewItem: BalanceViewItem)
        fun onChartClicked(viewItem: BalanceViewItem)
        fun onItemClicked(viewItem: BalanceViewItem)
        fun onSyncErrorClicked(viewItem: BalanceViewItem)
        fun onSwiped(viewItem: BalanceViewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BalanceItemViewHolder(inflate(parent, R.layout.view_holder_balance_item), listener)
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder !is BalanceItemViewHolder) return

        val item = getItem(position)
        val prev = payloads.lastOrNull() as? BalanceViewItem

        if (prev == null) {
            holder.bind(item)
        } else {
            holder.bindUpdate(item, prev)
        }
    }
}
