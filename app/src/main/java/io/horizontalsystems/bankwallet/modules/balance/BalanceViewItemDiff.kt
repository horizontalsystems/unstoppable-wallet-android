package io.horizontalsystems.bankwallet.modules.balance

import androidx.recyclerview.widget.DiffUtil

class BalanceViewItemDiff : DiffUtil.ItemCallback<BalanceViewItem>() {

    override fun areItemsTheSame(oldItem: BalanceViewItem, newItem: BalanceViewItem): Boolean {
        return oldItem.wallet == newItem.wallet
    }

    override fun areContentsTheSame(oldItem: BalanceViewItem, newItem: BalanceViewItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: BalanceViewItem, newItem: BalanceViewItem): Any? {
        return oldItem
    }
}
