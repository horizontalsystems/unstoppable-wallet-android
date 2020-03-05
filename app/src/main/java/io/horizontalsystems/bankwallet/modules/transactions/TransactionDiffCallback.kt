package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil

class TransactionDiffCallback(private val oldTxList: List<TransactionViewItem>, private val newTxList: MutableList<TransactionViewItem>)
    : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldTxList.size
    }

    override fun getNewListSize(): Int {
        return newTxList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldTxList[oldItemPosition] == newTxList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldTxList[oldItemPosition].record
        val new = newTxList[newItemPosition].record

        return old.blockHeight == new.blockHeight &&
                old.timestamp == new.timestamp &&
                old.interTransactionIndex == new.interTransactionIndex &&
                old.failed == new.failed &&
                old.conflictingTxHash == new.conflictingTxHash
    }
}
