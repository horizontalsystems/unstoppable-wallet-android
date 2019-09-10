package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.eoskit.EosKit
import io.horizontalsystems.eoskit.core.exceptions.BackendError
import io.horizontalsystems.eoskit.models.Transaction
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.RoundingMode

class EosAdapter(eos: CoinType.Eos, private val eosKit: EosKit, private val decimal: Int) : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter, ISendEosAdapter {

    private val token = eosKit.register(eos.token, eos.symbol)
    private val irreversibleThreshold = 330

    // IAdapter

    override fun start() {
        // started via EosKitManager
    }

    override fun stop() {
        eosKit.unregister(token)
    }

    override fun refresh() {
        // refreshed via EosKitManager
    }

    override val debugInfo: String = ""

    // ITransactionsAdapter

    override val confirmationsThreshold: Int = irreversibleThreshold

    override val lastBlockHeight: Int?
        get() = eosKit.irreversibleBlockHeight?.let { it + confirmationsThreshold }

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = eosKit.irreversibleBlockFlowable.map { Unit }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = token.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }

    override fun getTransactions(from: Pair<String, Int>?, limit: Int): Single<List<TransactionRecord>> {
        return eosKit.transactions(token, from?.second, limit).map { list ->
            list.map { transactionRecord(it) }
        }
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
                timestamp = transaction.date / 1000,
                from = listOf(from),
                to = listOf(to)
        )
    }

    // IBalanceAdapter

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

    // ISendEosAdapter

    override val availableBalance: BigDecimal
        get() = balance

    override fun validate(account: String) {
        // TODO need to implement this in EOS kit
    }

    override fun send(amount: BigDecimal, account: String, memo: String?): Single<Unit> {
        val scaledAmount = amount.setScale(decimal, RoundingMode.HALF_EVEN)
        return eosKit.send(token, account, scaledAmount, memo ?: "")
                .onErrorResumeNext { Single.error(getException(it)) }
                .map { Unit }
    }

    private fun getException(error: Throwable): Exception {
        return when (error) {
            is BackendError.TransferToSelfError -> CoinException(R.string.Eos_Backend_Error_SelfTransfer)
            is BackendError.AccountNotExistError -> CoinException(R.string.Eos_Backend_Error_AccountNotExist)
            is BackendError.InsufficientRamError -> CoinException(R.string.Eos_Backend_Error_InsufficientRam)
            is BackendError -> CoinException(null, error.detail)
            else -> Exception()
        }
    }

    // IReceiveAdapter

    override val receiveAddress: String get() = eosKit.account

    companion object {
        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) EosKit.NetworkType.TestNet else EosKit.NetworkType.MainNet
            EosKit.clear(App.instance, networkType, walletId)
        }

        fun validatePrivateKey(key: String) {
            EosKit.validatePrivateKey(key)
        }
    }

}
