package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class BitcoinAdapter(override val coin: Coin, authData: AuthData, newWallet: Boolean, testMode: Boolean)
    : IAdapter, BitcoinKit.Listener {
    override val feeCoinCode: String? = null
    override val decimal = 8

    private val bitcoinKit: BitcoinKit
    private val satoshisInBitcoin = Math.pow(10.0, decimal.toDouble()).toBigDecimal()

    init {
        val networkType: BitcoinKit.NetworkType =
                when (coin.type) {
                    is CoinType.Bitcoin -> {
                        if (testMode) BitcoinKit.NetworkType.TestNet else BitcoinKit.NetworkType.MainNet
                    }
                    is CoinType.BitcoinCash -> {
                        if (testMode) BitcoinKit.NetworkType.TestNetBitCash else BitcoinKit.NetworkType.MainNetBitCash
                    }
                    else -> throw Exception("Not supported Coin Type ${coin.type} for BitcoinAdapter")
                }
        bitcoinKit = BitcoinKit(authData.seed, networkType, newWallet = newWallet, walletId = authData.walletId)
    }

    override val balance: BigDecimal
        get() = bitcoinKit.balance.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN)
    override val balanceUpdatedSignal = PublishSubject.create<Unit>()

    override var state: AdapterState = AdapterState.Syncing(0, null)
        set(value) {
            field = value
            adapterStateUpdatedSubject.onNext(Unit)
        }
    override var adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override val confirmationsThreshold: Int = 6
    override val lastBlockHeight get() = bitcoinKit.lastBlockInfo?.height
    override val lastBlockHeightUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val debugInfo: String = ""

    override val receiveAddress: String get() = bitcoinKit.receiveAddress()

    override fun start() {
        bitcoinKit.listener = this
        bitcoinKit.start()
    }

    override fun stop() {
        bitcoinKit.stop()
    }

    override fun refresh() {
        bitcoinKit.refresh()
    }

    override fun clear() {
        bitcoinKit.clear()
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        val paymentData = bitcoinKit.parsePaymentAddress(address)
        return PaymentRequestAddress(paymentData.address, paymentData.amount?.toBigDecimal())
    }

    override fun send(address: String, value: BigDecimal, completion: ((Throwable?) -> (Unit))?) {
        try {
            bitcoinKit.send(address, (value * satoshisInBitcoin).toLong())
            completion?.invoke(null)
        } catch (ex: Exception) {
            completion?.invoke(ex)
        }
    }

    override fun fee(value: BigDecimal, address: String?): BigDecimal {
        try {
            val amount = (value * satoshisInBitcoin).toLong()
            val fee = bitcoinKit.fee(amount, address, true)
            return fee.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: UnspentOutputSelector.Error.InsufficientUnspentOutputs) {
            return e.fee.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: Exception) {
            return BigDecimal.ZERO
        }
    }

    override fun availableBalance(address: String?): BigDecimal {
        return BigDecimal.ZERO.max(balance.subtract(fee(balance, address)))
    }

    override fun validate(amount: BigDecimal, address: String?): List<SendStateError> {
        val errors = mutableListOf<SendStateError>()
        if (amount > availableBalance(address)) {
            errors.add(SendStateError.InsufficientAmount)
        }
        return errors
    }

    override fun validate(address: String) {
        bitcoinKit.validateAddress(address)
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return bitcoinKit.transactions(hashFrom, limit).map { it.map { transactionRecord(it) } }
    }

    //
    // BitcoinKit Listener implementations
    //
    override fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long) {
        balanceUpdatedSignal.onNext(Unit)
    }

    override fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo) {
        lastBlockHeightUpdatedSignal.onNext(Unit)
    }

    override fun onKitStateUpdate(bitcoinKit: BitcoinKit, state: BitcoinKit.KitState) {
        when (state) {
            is BitcoinKit.KitState.Synced -> {
                if (this.state !is AdapterState.Synced) {
                    this.state = AdapterState.Synced
                }
            }
            is BitcoinKit.KitState.NotSynced -> {
                if (this.state !is AdapterState.NotSynced) {
                    this.state = AdapterState.NotSynced
                }
            }
            is BitcoinKit.KitState.Syncing -> {
                this.state.let { currentState ->
                    val newProgress = (state.progress * 100).toInt()
                    val newDate = bitcoinKit.lastBlockInfo?.timestamp?.let { Date(it * 1000) }

                    if (currentState is AdapterState.Syncing && currentState.progress == newProgress) {
                        val currentDate = currentState.lastBlockDate
                        if (newDate != null && currentDate != null && DateHelper.isSameDay(newDate, currentDate)) {
                            return
                        }
                    }

                    this.state = AdapterState.Syncing(newProgress, newDate)
                }
            }
        }
    }

    override fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        // ignored for now
    }

    private fun transactionRecord(transaction: TransactionInfo) =
            TransactionRecord(
                    transaction.transactionHash,
                    transaction.blockHeight?.toLong() ?: 0,
                    transaction.amount.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN),
                    transaction.timestamp,
                    transaction.from.map { TransactionAddress(it.address, it.mine) },
                    transaction.to.map { TransactionAddress(it.address, it.mine) }
            )

}
