package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil

class TransactionViewItemDiff : DiffUtil.ItemCallback<TransactionViewItem>() {

    override fun areItemsTheSame(oldItem: TransactionViewItem, newItem: TransactionViewItem): Boolean {
        return oldItem.itemTheSame(newItem)
    }

    override fun areContentsTheSame(oldItem: TransactionViewItem, newItem: TransactionViewItem): Boolean {
        return oldItem.contentTheSame(newItem)
    }

    override fun getChangePayload(oldItem: TransactionViewItem, newItem: TransactionViewItem): Any? {
        return oldItem
    }

}
