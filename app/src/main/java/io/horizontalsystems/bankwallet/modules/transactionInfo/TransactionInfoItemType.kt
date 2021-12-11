package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import kotlinx.android.parcel.Parcelize
import java.util.*

sealed class TransactionInfoItemType {
    class TransactionType(val leftValue: String, val rightValue: String?) :
        TransactionInfoItemType()

    class Amount(val leftValue: String, val rightValue: ColoredValue) : TransactionInfoItemType()
    class Value(val title: String, val value: String) : TransactionInfoItemType()
    class Decorated(
        val title: String,
        val value: String,
        val actionButton: TransactionInfoActionButton? = null
    ) :
        TransactionInfoItemType()

    class Explorer(val title: String, val url: String?) : TransactionInfoItemType()

    class Status(val title: String, val leftIcon: Int, val status: TransactionStatusViewItem) :
        TransactionInfoItemType()

    class RawTransaction(val title: String, val actionButton: TransactionInfoActionButton? = null) :
        TransactionInfoItemType()

    class LockState(
        val title: String,
        val leftIcon: Int,
        val date: Date,
        val showLockInfo: Boolean
    ) : TransactionInfoItemType()

    class DoubleSpend(
        val title: String,
        val leftIcon: Int,
        val transactionHash: String,
        val conflictingHash: String
    ) : TransactionInfoItemType()

    class Options(
        val title: String,
        val optionButtonOne: TransactionInfoOption,
        val optionButtonTwo: TransactionInfoOption,
    ) : TransactionInfoItemType()
}

data class TransactionInfoOption(
    val title: String,
    val type: Type
) {
    @Parcelize
    enum class Type: Parcelable {
        SpeedUp, Cancel
    }
}

sealed class TransactionInfoActionButton {
    class ShareButton(val value: String) : TransactionInfoActionButton()
    object CopyButton : TransactionInfoActionButton()

    fun getIcon(): Int {
        return when (this) {
            is ShareButton -> R.drawable.ic_share_20
            CopyButton -> R.drawable.ic_copy_20
        }
    }
}

data class ColoredValue(val value: String, val color: Int)
