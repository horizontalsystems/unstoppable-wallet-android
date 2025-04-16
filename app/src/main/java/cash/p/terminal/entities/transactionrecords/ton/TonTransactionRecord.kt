package cash.p.terminal.entities.transactionrecords.ton

import cash.p.terminal.core.adapters.TonAdapter
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.tonkit.models.Event
import kotlin.math.absoluteValue

class TonTransactionRecord(
    source: TransactionSource,
    event: Event,
    val token: Token,
    val actions: List<Action>
) : TransactionRecord(
    uid = event.id,
    transactionHash = event.id,
    transactionIndex = 0,
    blockHeight = null,
    confirmationsThreshold = null,
    timestamp = event.timestamp,
    failed = false,
    spam = event.scam,
    source = source,
    transactionRecordType = TransactionRecordType.TON
) {
    val lt = event.lt
    val inProgress = event.inProgress
    val fee = TransactionValue.CoinValue(token, TonAdapter.Companion.getAmount(event.extra.absoluteValue))

    override fun status(lastBlockHeight: Int?) = when {
        inProgress -> TransactionStatus.Pending
        else -> TransactionStatus.Completed
    }

    override val mainValue: TransactionValue?
        get() = actions.singleOrNull()?.let { action ->
            when (val type = action.type) {
                is Action.Type.Receive -> type.value
                is Action.Type.Send -> type.value
                is Action.Type.Burn -> type.value
                is Action.Type.Mint -> type.value
                is Action.Type.ContractCall -> type.value
                is Action.Type.ContractDeploy,
                is Action.Type.Swap,
                is Action.Type.Unsupported -> null
            }
        }

    data class Action(
        val type: Type,
        val status: TransactionStatus
    ) {
        sealed class Type {
            data class Send(
                val value: TransactionValue,
                val to: String,
                val sentToSelf: Boolean,
                val comment: String?,
            ) : Type()

            data class Receive(
                val value: TransactionValue,
                val from: String,
                val comment: String?,
            ) : Type()

            data class Burn(val value: TransactionValue) : Type()

            data class Mint(val value: TransactionValue) : Type()

            data class Swap(
                val routerName: String?,
                val routerAddress: String,
                val valueIn: TransactionValue,
                val valueOut: TransactionValue
            ) : Type()

            data class ContractDeploy(val interfaces: List<String>) : Type()

            data class ContractCall(
                val address: String,
                val value: TransactionValue,
                val operation: String
            ) : Type()

            data class Unsupported(val type: String) : Type()
        }
    }
}