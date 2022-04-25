package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.os.Parcelable
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import kotlinx.parcelize.Parcelize
import java.util.*

sealed class TransactionInfoViewItem {
    class Transaction(val leftValue: String, val rightValue: String?) :
        TransactionInfoViewItem()

    class Amount(val leftValue: String, val rightValue: ColoredValue) : TransactionInfoViewItem()
    class Value(val title: String, val value: String) : TransactionInfoViewItem()
    class Decorated(
        val title: String,
        val value: String,
        val actionButton: TransactionInfoActionButton? = null
    ) :
        TransactionInfoViewItem()

    class Explorer(val title: String, val url: String?) : TransactionInfoViewItem()

    class Status(val title: String, val leftIcon: Int, val status: TransactionStatusViewItem) :
        TransactionInfoViewItem()

    class RawTransaction(val title: String, val actionButton: TransactionInfoActionButton? = null) :
        TransactionInfoViewItem()

    class LockState(
        val title: String,
        val leftIcon: Int,
        val date: Date,
        val showLockInfo: Boolean
    ) : TransactionInfoViewItem()

    class DoubleSpend(
        val title: String,
        val leftIcon: Int,
        val transactionHash: String,
        val conflictingHash: String
    ) : TransactionInfoViewItem()

    class Options(
        val title: String,
        val optionButtonOne: TransactionInfoOption,
        val optionButtonTwo: TransactionInfoOption,
    ) : TransactionInfoViewItem()
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

data class ColoredValueNew(val value: String, val color: ColorName)

enum class ColorName{
    Remus, Jacob, Grey, Leah;

    @Composable
    fun compose() = when (this) {
        Remus -> ComposeAppTheme.colors.remus
        Jacob -> ComposeAppTheme.colors.jacob
        Leah -> ComposeAppTheme.colors.leah
        Grey -> ComposeAppTheme.colors.grey
    }
}
