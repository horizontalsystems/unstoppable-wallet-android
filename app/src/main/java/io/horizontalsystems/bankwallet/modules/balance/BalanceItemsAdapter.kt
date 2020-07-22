package io.horizontalsystems.bankwallet.modules.balance

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_add_coin.*

class BalanceItemsAdapter(private val listener: Listener) : ListAdapter<BalanceViewItem, RecyclerView.ViewHolder>(BalanceViewItemDiff()) {

    interface Listener {
        fun onSendClicked(viewItem: BalanceViewItem)
        fun onReceiveClicked(viewItem: BalanceViewItem)
        fun onSwapClicked(viewItem: BalanceViewItem)
        fun onChartClicked(viewItem: BalanceViewItem)
        fun onItemClicked(viewItem: BalanceViewItem)
        fun onSyncErrorClicked(viewItem: BalanceViewItem)
        fun onAddCoinClicked()
    }

    private val coinType = 1
    private val addCoinType = 2

    override fun getItemCount() = super.getItemCount() + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) addCoinType else coinType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            addCoinType -> AddCoinViewHolder(inflate(parent, R.layout.view_holder_add_coin))
            else -> BalanceItemViewHolder(inflate(parent, R.layout.view_holder_balance_item), listener)
        }
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder is AddCoinViewHolder) {
            holder.manageCoins.setOnSingleClickListener { listener.onAddCoinClicked() }
        }

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

class AddCoinViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
