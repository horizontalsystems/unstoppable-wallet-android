package io.horizontalsystems.bankwallet.modules.transactions

import android.text.TextPaint
import android.text.TextUtils
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState

object TransactionViewHelper {

    private val paint = TextPaint()

    fun getLockIcon(lockState: TransactionLockState?) = when {
        lockState == null -> 0
        lockState.locked -> R.drawable.ic_lock_20
        else -> R.drawable.ic_unlock_20
    }

    fun truncated(string: String, available: Float): CharSequence {
        return TextUtils.ellipsize(string, paint, available, TextUtils.TruncateAt.MIDDLE)
    }

}
