package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionLockState
import io.horizontalsystems.views.helpers.LayoutHelper

object TransactionViewHelper {

    fun getLockIcon(lockState: TransactionLockState?) = when {
        lockState == null -> 0
        lockState.locked -> R.drawable.ic_lock
        else -> R.drawable.ic_unlock
    }

    fun getAmountColor(type: TransactionType, context: Context): Int {
        return when (type) {
            TransactionType.Outgoing, TransactionType.SentToSelf -> {
                LayoutHelper.getAttr(R.attr.ColorJacob, context.theme)
                        ?: context.getColor(R.color.yellow_d)
            }
            TransactionType.Incoming -> {
                LayoutHelper.getAttr(R.attr.ColorRemus, context.theme)
                        ?: context.getColor(R.color.green_d)
            }
            TransactionType.Approve -> {
                LayoutHelper.getAttr(R.attr.ColorLeah, context.theme)
                        ?: context.getColor(R.color.steel_light)
            }
        }
    }

    fun getTransactionTypeIcon(type: TransactionType): Int {
        return when (type) {
            TransactionType.Outgoing, TransactionType.SentToSelf -> R.drawable.ic_outgoing
            TransactionType.Incoming -> R.drawable.ic_incoming
            TransactionType.Approve -> R.drawable.ic_swap_approval
        }
    }
}
