package cash.p.terminal.entities.transactionrecords.stellar

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.stellarkit.room.Operation

class StellarTransactionRecord(
    baseToken: Token,
    source: TransactionSource,
    val operation: Operation,
    val type: Type,
) : TransactionRecord(
    uid = operation.id.toString(),
    transactionHash = operation.transactionHash,
    transactionIndex = 0,
    blockHeight = null,
    confirmationsThreshold = null,
    timestamp = operation.timestamp,
    failed = !operation.transactionSuccessful,
    spam = false,
    source = source,
    transactionRecordType = type.toTransactionRecordType(),
    token = baseToken,
    to = type.to,
    from = type.from,
    sentToSelf = type.sentToSelf,
    memo = operation.memo,
) {
    override val mainValue = type.mainValue
    val fee = operation.fee?.let { TransactionValue.CoinValue(baseToken, it) }

    sealed class Type {
        data class Send(
            val value: TransactionValue,
            override val to: String,
            override val sentToSelf: Boolean,
            val comment: String?,
            val accountCreated: Boolean,
        ) : Type()

        data class Receive(
            val value: TransactionValue,
            override val from: String,
            val comment: String?,
            val accountCreated: Boolean,
        ) : Type()

        data class ChangeTrust(
            val trustee: String,
            val value: TransactionValue
        ) : Type()

        class Unsupported(val type: String) : Type()

        val mainValue: TransactionValue?
            get() = when (this) {
                is Receive -> value
                is Send -> value
                is ChangeTrust -> value
                is Unsupported -> null
            }

        open val to: String?
            get() = when (this) {
                is Send -> to
                is ChangeTrust -> trustee
                else -> null
            }

        open val from: String?
            get() = when (this) {
                is Receive -> from
                else -> null
            }

        open val sentToSelf: Boolean
            get() = when (this) {
                is Send -> sentToSelf
                else -> false
            }

        fun toTransactionRecordType() =
            when (this) {
                is Send -> TransactionRecordType.STELLAR_INCOMING
                is Receive -> TransactionRecordType.STELLAR_OUTGOING
                is ChangeTrust -> TransactionRecordType.UNKNOWN
                is Unsupported -> TransactionRecordType.UNKNOWN
            }
    }

    override fun status(lastBlockHeight: Int?) = if (failed) {
        TransactionStatus.Failed
    } else {
        TransactionStatus.Completed
    }
}