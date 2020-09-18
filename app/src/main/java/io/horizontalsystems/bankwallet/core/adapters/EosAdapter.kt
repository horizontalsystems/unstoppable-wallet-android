package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.eoskit.EosKit
import io.horizontalsystems.eoskit.core.exceptions.BackendError
import io.horizontalsystems.eoskit.models.Transaction
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
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

    override fun getReceiveAddressType(wallet: Wallet): String? = null

    // ITransactionsAdapter

    override val confirmationsThreshold: Int = irreversibleThreshold

    override val lastBlockInfo: LastBlockInfo?
        get() = eosKit.irreversibleBlockHeight?.let { LastBlockInfo(it + confirmationsThreshold) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = eosKit.irreversibleBlockFlowable.map { Unit }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = token.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }

    override fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>> {
        return eosKit.transactions(token, from?.interTransactionIndex, limit).map { list ->
            list.map { transactionRecord(it) }
        }
    }

    private fun transactionRecord(transaction: Transaction): TransactionRecord {
        val myAddress = eosKit.account
        val fromMine = transaction.from == myAddress
        val toMine = transaction.to == myAddress

        val type = when {
            fromMine && toMine -> TransactionType.SentToSelf
            fromMine -> TransactionType.Outgoing
            else -> TransactionType.Incoming
        }

        return TransactionRecord(
                uid = transaction.id,
                transactionHash = transaction.id,
                transactionIndex = 0,
                interTransactionIndex = transaction.actionSequence,
                blockHeight = transaction.blockNumber.toLong(),
                amount = transaction.amount?.toBigDecimal() ?: BigDecimal.ZERO,
                timestamp = transaction.date / 1000,
                from = transaction.from,
                to = transaction.to,
                type = type
        )
    }

    // IBalanceAdapter

    override val state: AdapterState
        get() = when (val kitSyncState = token.syncState) {
            EosKit.SyncState.Synced -> AdapterState.Synced
            is EosKit.SyncState.NotSynced -> AdapterState.NotSynced(kitSyncState.error)
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
        val disposable = Single.fromCallable {
            eosKit.validate(account)
        }
                .subscribeOn(Schedulers.io())
                .blockingGet()
    }

    override fun send(amount: BigDecimal, account: String, memo: String?, logger: AppLogger): Single<Unit> {
        val scaledAmount = amount.setScale(decimal, RoundingMode.HALF_EVEN)
        return eosKit.send(token, account, scaledAmount, memo ?: "")
                .doOnSubscribe {
                    logger.info("call eosKit.send")
                }
                .onErrorResumeNext { Single.error(getException(it)) }
                .map { Unit }
    }

    private fun getException(error: Throwable): Exception {
        return when (error) {
            is BackendError.TransferToSelfError -> LocalizedException(R.string.Eos_Backend_Error_SelfTransfer)
            is BackendError.AccountNotExistError -> LocalizedException(R.string.Eos_Backend_Error_AccountNotExist)
            is BackendError.InsufficientRamError -> LocalizedException(R.string.Eos_Backend_Error_InsufficientRam)
            is BackendError -> Exception(error.detail)
            else -> Exception(error.message)
        }
    }

    // IReceiveAdapter

    override val receiveAddress: String get() = eosKit.account

    companion object {
        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) EosKit.NetworkType.TestNet else EosKit.NetworkType.MainNet
            EosKit.clear(App.instance, networkType, walletId)
        }

        fun validateAccountName(accountName: String) {
            if (accountName.length !in 1..12) {
                throw EosError.InvalidAccountName
            }
        }

        fun validatePrivateKey(key: String) {
            EosKit.validatePrivateKey(key)
        }
    }

    sealed class EosError: Exception(){
        object InvalidAccountName: EosError()
    }

}
