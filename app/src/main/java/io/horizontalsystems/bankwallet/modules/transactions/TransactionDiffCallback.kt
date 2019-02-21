package io.horizontalsystems.bankwallet.modules.transactions

import android.support.v7.util.DiffUtil
import io.horizontalsystems.bankwallet.entities.TransactionItem

class TransactionDiffCallback(private val oldTxList: List<TransactionItem>, private val newTxList: List<TransactionItem>)
    : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldTxList.size
    }

    override fun getNewListSize(): Int {
        return newTxList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldTxList[oldItemPosition].coinCode == newTxList[newItemPosition].coinCode
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldTxList[oldItemPosition].record == newTxList[newItemPosition].record
    }
}
