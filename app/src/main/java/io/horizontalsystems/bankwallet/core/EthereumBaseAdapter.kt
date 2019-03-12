package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

abstract class EthereumBaseAdapter(override val coin: Coin, protected val ethereumKit: EthereumKit, final override val decimal: Int)
    : IAdapter, EthereumKit.Listener {

    private val disposables = CompositeDisposable()

    //
    // Adapter
    //
    override val feeCoinCode: String? = null
    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val balanceUpdatedSignal = PublishSubject.create<Unit>()
    override val adapterStateUpdatedSubject = PublishSubject.create<Unit>()
    override val lastBlockHeightUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()


    override var state: AdapterState = AdapterState.Synced
        set(value) {
            field = value
            adapterStateUpdatedSubject.onNext(Unit)
        }

    override val confirmationsThreshold: Int = 12
    override val lastBlockHeight: Int? get() = ethereumKit.lastBlockHeight

    override val debugInfo: String = ""

    override val receiveAddress: String get() = ethereumKit.receiveAddress

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return PaymentRequestAddress(address)
    }

    override fun validate(address: String) {
        ethereumKit.validateAddress(address)
    }

    open val balanceString: String?
        get() {
            return null
        }

    override fun send(address: String, value: BigDecimal, feePriority: FeeRatePriority, completion: ((Throwable?) -> Unit)?) {
        sendSingle(address, value)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    completion?.invoke(null)
                }, {
                    completion?.invoke(it)
                })?.let { disposables.add(it) }
    }

    protected fun balanceInBigDecimal(balanceString: String?, decimal: Int): BigDecimal {
        balanceString?.toBigDecimalOrNull()?.let {
            val converted = it.movePointLeft(decimal)
            return converted.stripTrailingZeros()
        } ?: return BigDecimal.ZERO
    }

    private fun sendSingle(address: String, amount: BigDecimal): Single<Unit> {
        val poweredDecimal = amount.scaleByPowerOfTen(decimal)
        val noScaleDecimal = poweredDecimal.setScale(0, RoundingMode.HALF_DOWN)

        return sendSingle(address, noScaleDecimal.toPlainString())
    }

    open fun sendSingle(address: String, amount: String): Single<Unit> {
        return Single.just(Unit)
    }

    //
    // EthereumKit Listener
    //

    override fun onBalanceUpdate() {
        balanceUpdatedSignal.onNext(Unit)
    }

    override fun onLastBlockHeightUpdate() {
        lastBlockHeightUpdatedSignal.onNext(Unit)
    }

    override fun onSyncStateUpdate() {}

    override fun onTransactionsUpdate(transactions: List<EthereumTransaction>) {
        val transactionRecords = transactions.map { transactionRecord(it) }
        transactionRecordsSubject.onNext(transactionRecords)
    }


    //
    // Helpers
    //

    protected fun convertState(kitState: EthereumKit.SyncState): AdapterState {
        return when (kitState) {
            is EthereumKit.SyncState.Synced -> AdapterState.Synced
            is EthereumKit.SyncState.NotSynced -> AdapterState.NotSynced
            is EthereumKit.SyncState.Syncing -> AdapterState.Synced
        }
    }

    protected fun transactionRecord(transaction: EthereumTransaction): TransactionRecord {

        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from, transaction.from == mineAddress)

        val to = TransactionAddress(transaction.to, transaction.to == mineAddress)

        var amount: BigDecimal = BigDecimal.valueOf(0.0)

        transaction.value.toBigDecimalOrNull()?.let {
            amount = it.movePointLeft(decimal)
            if (from.mine) {
                amount = amount.unaryMinus()
            }
        }

        return TransactionRecord(
                transaction.hash,
                transaction.blockNumber,
                amount,
                transaction.timeStamp,
                listOf(from),
                listOf(to)
        )
    }

}
