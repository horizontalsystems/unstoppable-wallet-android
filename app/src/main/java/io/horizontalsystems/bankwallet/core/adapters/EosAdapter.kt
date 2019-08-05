package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.eoskit.EosKit
import io.horizontalsystems.eoskit.models.Transaction
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

class EosAdapter(override val wallet: Wallet, eos: CoinType.Eos, private val eosKit: EosKit) : IAdapter {

    private val token = eosKit.register(eos.token, eos.symbol)
    private val irreversibleThreshold = 330

    override val decimal: Int = 4

    override val feeCoinCode: String? = eos.symbol

    override val confirmationsThreshold: Int = irreversibleThreshold

    override fun start() {
        // started via EosKitManager
    }

    override fun stop() {
        // stopped via EosKitManager
    }

    override fun refresh() {
        // refreshed via EosKitManager
    }

    override val lastBlockHeight: Int?
        get() = eosKit.irreversibleBlockHeight?.let { it + confirmationsThreshold }

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = eosKit.irreversibleBlockFlowable.map { Unit }

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

    override fun send(params: Map<SendModule.AdapterFields, Any?>): Single<Unit> {
        val coinValue = params[SendModule.AdapterFields.CoinValue] as? CoinValue
                ?: throw WrongParameters()
        val address = params[SendModule.AdapterFields.Address] as? String
                ?: throw WrongParameters()
        val memo = params[SendModule.AdapterFields.Memo] as? String ?: ""

        return eosKit.send(token, address, coinValue.value, memo).map { Unit }
    }

    override fun fee(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        return BigDecimal.ZERO
    }

    override fun getFeeRate(feeRatePriority: FeeRatePriority): Long {
        return 0L
    }

    override fun availableBalance(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        return balance
    }

    override fun validate(address: String) {
    }

    override fun validate(params: Map<SendModule.AdapterFields, Any?>): List<SendStateError> {
        val amount = params[SendModule.AdapterFields.CoinAmountInBigDecimal] as? BigDecimal
                ?: throw WrongParameters()

        val errors = mutableListOf<SendStateError>()
        val availableBalance = availableBalance(params)
        if (availableBalance < amount) {
            errors.add(SendStateError.InsufficientAmount(availableBalance))
        }

        return errors
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        var addressError: AddressError.InvalidPaymentAddress? = null
        try {
            validate(address)
        } catch (e: Exception) {
            addressError = AddressError.InvalidPaymentAddress()
        }
        return PaymentRequestAddress(address, null, error = addressError)
    }

    override val receiveAddress: String get() = eosKit.account

    override val debugInfo: String = ""

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
                timestamp = transaction.date / 1000,
                from = listOf(from),
                to = listOf(to)
        )
    }

    companion object {
        fun clear(context: Context) {
        }

        fun validatePrivateKey(key: String) {
            EosKit.validatePrivateKey(key)
        }
    }
}
