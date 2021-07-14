package io.horizontalsystems.bankwallet.modules.transactionInfo

sealed class TransactionInfoItemType {
    class TransactionType(val leftValue: String, val rightValue: String?) :
        TransactionInfoItemType()

    class Amount(val leftValue: String, val rightValue: ColoredValue) : TransactionInfoItemType()
    class Value(val title: String, val value: String) : TransactionInfoItemType()
    class Decorated(val title: String, val value: String, val showShare: Boolean = false) :
        TransactionInfoItemType()

    class Button(val title: String, val leftIcon: Int, val type: TransactionInfoButtonType) :
        TransactionInfoItemType()

    class Status(val title: String, val status: TransactionStatusViewItem) :
        TransactionInfoItemType()
}

sealed class TransactionInfoButtonType {
    class OpenExplorer(val url: String?) : TransactionInfoButtonType()
    object RevokeApproval : TransactionInfoButtonType()
    object Resend : TransactionInfoButtonType()
}

class ColoredValue(val value: String, val color: Int)
