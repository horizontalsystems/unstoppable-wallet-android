package io.horizontalsystems.bankwallet.modules.transactions

import android.support.v7.util.DiffUtil


class TransactionViewItemDiffCallback(private val oldTxList: List<TransactionViewItemCache>, private val newTxList: List<TransactionViewItemCache>)
    : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldTxList.size
    }

    override fun getNewListSize(): Int {
        return newTxList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldTxList[oldItemPosition].transactionHash == newTxList[newItemPosition].transactionHash
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldTxList[oldItemPosition]
        val new = newTxList[newItemPosition]
        return old.fiatValueString == new.fiatValueString && old.status.equals(new.status) && old.dateString == new.dateString
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val old = oldTxList[oldItemPosition]
        val new = newTxList[newItemPosition]

        val diff = mutableListOf<String>()
        if (new.status!= old.status) {
            diff.add("status")
        }
        if (new.dateString != old.dateString) {
            diff.add("date")
        }
        if (new.fiatValueString != old.fiatValueString) {
            diff.add("fiatValue")
        }

        return diff
    }
}
