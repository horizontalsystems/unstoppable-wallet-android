package cash.p.terminal.entities.transactionrecords.tron

import cash.p.terminal.core.adapters.BaseTronAdapter
import cash.p.terminal.core.managers.SpamManager
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.tronkit.models.Transaction

class TronTransactionRecord(
    val spender: String? = null,
    val value: TransactionValue? = null,
    val contractAddress: String? = null,
    val method: String? = null,
    val incomingEvents: List<TransferEvent>? = null,
    val outgoingEvents: List<TransferEvent>? = null,
    spamManager: SpamManager? = null,
    val from: String? = null,
    val to: String? = null,
    val sentToSelf: Boolean = false,
    val transaction: Transaction,
    val token: Token,
    source: TransactionSource,
    val foreignTransaction: Boolean = false,
    spam: Boolean = false,
    transactionRecordType: TransactionRecordType
) : TransactionRecord(
    uid = transaction.hashString,
    transactionHash = transaction.hashString,
    transactionIndex = 0,
    blockHeight = transaction.blockNumber?.toInt(),
    confirmationsThreshold = BaseTronAdapter.confirmationsThreshold,
    timestamp = transaction.timestamp / 1000,
    failed = transaction.isFailed,
    spam = spam,
    source = source,
    transactionRecordType = transactionRecordType
) {

    val fee: TransactionValue?

    init {
        val feeAmount: Long? = transaction.fee
        fee = if (feeAmount != null) {
            val feeDecimal = feeAmount.toBigDecimal()
                .movePointLeft(token.decimals).stripTrailingZeros()

            TransactionValue.CoinValue(token, feeDecimal)
        } else {
            null
        }
    }

    override fun status(lastBlockHeight: Int?): TransactionStatus {
        when {
            failed -> {
                return TransactionStatus.Failed
            }

            transaction.confirmed -> {
                return TransactionStatus.Completed
            }

            blockHeight != null && lastBlockHeight != null -> {
                val threshold = confirmationsThreshold ?: 1
                val confirmations = lastBlockHeight - blockHeight.toInt() + 1

                return if (confirmations >= threshold) {
                    TransactionStatus.Completed
                } else {
                    TransactionStatus.Processing(confirmations.toFloat() / threshold.toFloat())
                }
            }

            else -> return TransactionStatus.Pending
        }
    }

    override val mainValue: TransactionValue?
        get() {
            return if (transactionRecordType == TransactionRecordType.TRON_APPROVE ||
                transactionRecordType == TransactionRecordType.TRON_INCOMING ||
                transactionRecordType == TransactionRecordType.TRON_OUTGOING
            ) {
                value
            } else if (transactionRecordType == TransactionRecordType.TRON_CONTRACT_CALL ||
                transactionRecordType == TransactionRecordType.TRON_EXTERNAL_CONTRACT_CALL
            ) {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(
                    incomingEvents!!,
                    outgoingEvents!!
                )

                when {
                    (incomingValues.isEmpty() && outgoingValues.size == 1) -> outgoingValues.first()
                    (incomingValues.size == 1 && outgoingValues.isEmpty()) -> incomingValues.first()
                    else -> null
                }
            } else {
                null
            }
        }

}
