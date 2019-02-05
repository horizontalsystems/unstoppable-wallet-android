package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

abstract class EthereumBaseAdapter(protected val ethereumKit: EthereumKit, final override val decimal: Int)
    : IAdapter, EthereumKit.Listener {

    private val weisInEther = Math.pow(10.0, decimal.toDouble()).toBigDecimal()

    //
    // Adapter
    //
    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()
    override val balanceUpdatedSignal = PublishSubject.create<Unit>()
    override var state: AdapterState = AdapterState.Syncing(null)
        set(value) {
            field = value
            stateUpdatedSignal.onNext(Unit)
        }
    override val stateUpdatedSignal = PublishSubject.create<Unit>()

    override val confirmationsThreshold: Int = 12
    override val lastBlockHeight: Int? get() = ethereumKit.lastBlockHeight
    override val lastBlockHeightUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    override val debugInfo: String = ""

    override val receiveAddress: String get() = ethereumKit.receiveAddress

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return PaymentRequestAddress(address)
    }

    override fun validate(address: String) {
        ethereumKit.validateAddress(address)
    }

    //
    // EthereumKit Listener
    //

    override fun onBalanceUpdate(balance: Double) {
        balanceUpdatedSignal.onNext(Unit)
    }

    override fun onLastBlockHeightUpdate(height: Int) {
        lastBlockHeightUpdatedSignal.onNext(Unit)
    }

    override fun onKitStateUpdate(state: EthereumKit.KitState) {
        when (state) {
            is EthereumKit.KitState.Synced -> {
                if (this.state != AdapterState.Synced) {
                    this.state = AdapterState.Synced
                }
            }
            is EthereumKit.KitState.NotSynced -> {
                if (this.state != AdapterState.NotSynced) {
                    this.state = AdapterState.NotSynced
                }
            }
            is EthereumKit.KitState.Syncing -> {
                if (this.state != AdapterState.Syncing(null)) {
                    this.state = AdapterState.Syncing(null)
                }
            }
        }
    }

    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    //
    // Helpers
    //

    protected fun transactionRecord(transaction: Transaction): TransactionRecord {
        val amountEther: BigDecimal = convertToValue(transaction.value) ?: BigDecimal.ZERO
        val mineAddress = ethereumKit.receiveAddress.toLowerCase()

        val from = TransactionAddress()
        from.address = transaction.from
        from.mine = transaction.from.toLowerCase() == mineAddress

        val to = TransactionAddress()
        to.address = transaction.to
        to.mine = transaction.to.toLowerCase() == mineAddress

        val transactionAmount = if (from.mine)
            amountEther.unaryMinus() else
            amountEther

        return TransactionRecord(
                transaction.hash,
                transaction.blockNumber,
                transactionAmount,
                transaction.timeStamp,
                listOf(from),
                listOf(to)
        )
    }

    private fun convertToValue(amount: String): BigDecimal? = try {
        BigDecimal(amount).divide(weisInEther, decimal, RoundingMode.HALF_EVEN)
    } catch (ex: Exception) {
        null
    }
}
