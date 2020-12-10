package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionLockState
import io.horizontalsystems.views.helpers.LayoutHelper

object TransactionViewHelper {

    fun getLockIcon(lockState: TransactionLockState?) = when {
        lockState == null -> 0
        lockState.locked -> R.drawable.ic_lock_20
        else -> R.drawable.ic_unlock_20
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
            TransactionType.Outgoing, TransactionType.SentToSelf -> R.drawable.ic_outgoing_20
            TransactionType.Incoming -> R.drawable.ic_incoming_20
            TransactionType.Approve -> R.drawable.ic_swap_approval_20
        }
    }

    fun getBottomIconImage(status: TransactionStatus): Int? {
        return when (status) {
            is TransactionStatus.Failed -> R.drawable.ic_attention_red_20
            is TransactionStatus.Completed -> R.drawable.ic_checkmark_20
            else -> null
        }
    }
}
