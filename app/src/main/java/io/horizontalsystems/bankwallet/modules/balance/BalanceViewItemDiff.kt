package io.horizontalsystems.bankwallet.modules.balance

import androidx.recyclerview.widget.DiffUtil

class BalanceViewItemDiff(private val oldItems: List<BalanceViewItem>, private val newItems: List<BalanceViewItem>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].wallet == newItems[newItemPosition].wallet
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return newItems[newItemPosition].updateType
    }
}
