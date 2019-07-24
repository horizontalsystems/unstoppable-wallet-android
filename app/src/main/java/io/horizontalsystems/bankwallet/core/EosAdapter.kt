package io.horizontalsystems.bankwallet.core

import android.content.Context
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.eoskit.EosKit
import io.horizontalsystems.eoskit.models.Transaction
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class EosAdapter(override val wallet: Wallet, eos: CoinType.Eos, kit: EosKit) : EosBaseAdapter(eos, kit) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override val state: AdapterState
        get() = when (token.syncState) {
            EosKit.SyncState.Synced -> AdapterState.Synced
            EosKit.SyncState.NotSynced -> AdapterState.NotSynced
            EosKit.SyncState.Syncing -> AdapterState.Syncing(50, null)
        }

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = token.syncStateFlowable.map { Unit }

    override val balance: BigDecimal
        get() = token.balance

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = token.balanceFlowable.map { Unit }

    override fun getTransactions(from: Pair<String, Int>?, limit: Int): Single<List<TransactionRecord>> {
        return eosKit.transactions(token, from?.second, limit).map { list ->
            list.map { transactionRecord(it) }
        }
    }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = token.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }

    override fun sendSingle(address: String, amount: String): Single<Unit> {
        return eosKit.send(token, address, amount.toBigDecimal(), "").map { Unit }
    }

    override fun fee(value: BigDecimal, address: String?, feePriority: FeeRatePriority): BigDecimal {
        return BigDecimal(0)
    }

    override fun availableBalance(address: String?, feePriority: FeeRatePriority): BigDecimal {
        return balance
    }

    override fun validate(amount: BigDecimal, address: String?, feePriority: FeeRatePriority): List<SendStateError> {
        val errors = mutableListOf<SendStateError>()
        if (amount > availableBalance(address, feePriority)) {
            errors.add(SendStateError.InsufficientAmount)
        }
        return errors
    }

    private fun transactionRecord(transaction: Transaction): TransactionRecord {
        val fromAddressHex = transaction.from
        val from = TransactionAddress(fromAddressHex!!, fromAddressHex == eosKit.account)

        val toAddressHex = transaction.to
        val to = TransactionAddress(toAddressHex!!, toAddressHex == eosKit.account)

        var amount = BigDecimal(0)

        transaction.amount?.toBigDecimal()?.let {
            amount = it

            if (from.mine) {
                amount = -amount
            }
        }

        return TransactionRecord(
                transactionHash = transaction.id,
                transactionIndex = 0,
                interTransactionIndex = transaction.actionSequence,
                blockHeight = transaction.blockNumber.toLong(),
                amount = amount,
                timestamp = dateFormat.parse(transaction.date).time / 1000,
                from = listOf(from),
                to = listOf(to)
        )
    }

    companion object {
        fun clear(context: Context) {
        }
    }
}
